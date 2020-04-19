package com.lyeeedar.MapGeneration

import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.Util.Array2D

class Area()
{
	constructor(width: Int, height: Int, grid: Array2D<Symbol>) : this()
	{
		this.allowedBoundsWidth = width
		this.allowedBoundsHeight = height
		this.width = width
		this.height = height

		this.grid = grid
	}

	var allowedBoundsX: Int = 0
	var allowedBoundsY: Int = 0
	var allowedBoundsWidth: Int = 0
	var allowedBoundsHeight: Int = 0

	var x: Int = 0
		set(value)
		{
			field = value
			if (value < allowedBoundsX || value > allowedBoundsX + allowedBoundsWidth)
			{
				throw Exception("Invalid area x '$value'!")
			}
		}

	var y: Int = 0
		set(value)
		{
			field = value
			if (value < allowedBoundsY || value > allowedBoundsY + allowedBoundsHeight)
			{
				throw Exception("Invalid area y '$value'!")
			}
		}

	var width: Int = 0
		set(value)
		{
			field = value
			if (value < 0 || value > allowedBoundsWidth)
			{
				throw Exception("Invalid area width '$value'!")
			}
		}

	var height: Int = 0
		set(value)
		{
			field = value
			if (value < 0 || value > allowedBoundsHeight)
			{
				throw Exception("Invalid area height '$value'!")
			}
		}

	lateinit var grid: Array2D<Symbol>

	var isPoints = false
	val points = Array<Pos>(false, 16)

	var flipX: Boolean = false
	var flipY: Boolean = false
	var orientation: Float = 0f
		get() = field
		set(value)
		{
			field = value

			orientationDirty = true
		}
	val mat: Matrix3 = Matrix3()
		get()
		{
			if (orientationDirty)
			{
				orientationDirty = false
				mat.setToRotation(orientation)
			}

			return field
		}

	val vec: Vector3 = Vector3()
	var orientationDirty = true

	var xMode: Boolean = true

	var pos: Int
		get() = if (xMode) x else y
		set(value)
		{
			if (xMode)
			{
				x = value
			}
			else
			{
				y = value
			}
		}

	var size: Int
		get() = if (xMode) width else height
		set(value)
		{
			if (xMode)
			{
				width = value
			}
			else
			{
				height = value
			}
		}

	val hasContents: Boolean
		get()
		{
			if (isPoints && points.size == 0) return false
			if (width == 0) return false
			if (height == 0) return false

			return true
		}

	fun writeVariables(variables: ObjectFloatMap<String>)
	{
		variables.put("x", x.toFloat())
		variables.put("y", y.toFloat())
		variables.put("width", width.toFloat())
		variables.put("height", height.toFloat())
		variables.put("size", size.toFloat())
		variables.put("pos", pos.toFloat())

		if (isPoints)
		{
			variables.put("count", points.size.toFloat())
		}
	}

	fun convertToPoints()
	{
		points.addAll(getAllPoints())
		isPoints = true
	}

	val tempPointsArray = Array<Pos>()
	fun getAllPoints(): Array<Pos>
	{
		if (isPoints) return points

		val allPoints = tempPointsArray
		allPoints.clear()

		for (ix in 0 until width)
		{
			for (iy in 0 until height)
			{
				allPoints.add(Pos(x + ix, y + iy))
			}
		}

		return allPoints
	}

	fun addPointsWithin(area: Area)
	{
		if (width == 0 || height == 0) return

		for (point in area.points)
		{
			if (point.x >= x && point.x < x+width && point.y >= y && point.y < y+height)
			{
				points.add(point)
			}
		}
	}

	fun copy(): Area
	{
		val area = Area()
		area.grid = grid
		area.allowedBoundsX = allowedBoundsX
		area.allowedBoundsY = allowedBoundsY
		area.allowedBoundsWidth = allowedBoundsWidth
		area.allowedBoundsHeight = allowedBoundsHeight

		area.x = x
		area.y = y
		area.width = width
		area.height = height
		area.flipX = flipX
		area.flipY = flipY
		area.orientation = orientation
		area.xMode = xMode
		area.isPoints = isPoints
		area.points.addAll(points)

		return area
	}

	operator fun get(x: Int, y: Int): Symbol?
	{
		val pos = localToWorld(x, y)
		return grid.tryGet(pos.x, pos.y, null)
	}

	operator fun get(pos: Pos): Symbol?
	{
		val pos = localToWorld(pos)
		return grid.tryGet(pos.x, pos.y, null)
	}

	fun localToWorld(x: Int, y: Int): Pos
	{
		if (orientation == 0f)
		{
			return Pos(this.x + x, this.y + y)
		}
		else
		{
			val cx = this.x + width / 2
			val cy = this.y + height / 2

			val lx = x - width / 2
			val ly = y - height / 2

			vec.set(lx.toFloat(), ly.toFloat(), 0f)

			vec.mul(mat)

			var dx = Math.round(vec.x)
			var dy = Math.round(vec.y)

			if (flipX) dx = (width - 1) - dx
			if (flipY) dy = (height - 1) - dy

			return Pos(dx + cx, dy + cy)
		}
	}

	fun localToWorld(pos: Pos): Pos
	{
		return localToWorld(pos.x - x, pos.y - y)
	}

	fun newAreaFromCharGrid(grid: kotlin.Array<CharArray>): Area
	{
		val newArea = this.copy()
		newArea.convertToPoints()
		newArea.points.clear()

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val char = grid[x][y]
				if (char != '#')
				{
					val pos = Pos(this.x + x, this.y + y)
					newArea.points.add(pos)
				}
			}
		}

		return newArea
	}

	companion object
	{
		val defaultVariables = ObjectFloatMap<String>()
		init
		{
			defaultVariables.put("x", 0f)
			defaultVariables.put("y", 0f)
			defaultVariables.put("width", 0f)
			defaultVariables.put("height", 0f)
			defaultVariables.put("size", 0f)
			defaultVariables.put("pos", 0f)
			defaultVariables.put("count", 0f)
		}
	}
}

class Pos(val x: Int, val y: Int)
{
	fun dst2(other: Pos): Float
	{
		return Vector2.dst2(x.toFloat(), y.toFloat(), other.x.toFloat(), other.y.toFloat())
	}
}