package com.lyeeedar.Pathfinding

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.BinaryHeap
import com.lyeeedar.Direction
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics

class AStarPathfind<T: IPathfindingTile>(private val grid: Array2D<T>)
{
	private val width: Int = grid.xSize
	private val height: Int = grid.ySize
	private val nodes: Array2D<Node>

	private var startx: Int = 0
	private var starty: Int = 0
	private var endx: Int = 0
	private var endy: Int = 0
	private var currentx: Int = 0
	private var currenty: Int = 0

	private var currentID: Int = 0

	private var findOptimal: Boolean = false
	private var actorSize: Int = 1
	private lateinit var travelType: SpaceSlot
	private lateinit var self: Any

	private val openList = BinaryHeap<Node>()

	private val path = Array<Point>()

	init
	{
		this.nodes = Array2D<Node>(width, height) { x,y -> Node(x, y) }
	}

	private fun path()
	{
		val current = openList.pop()

		currentx = current.x
		currenty = current.y

		if (isEnd(currentx, currenty))
		{
			return
		}

		for (offset in Direction.CardinalValues)
		{
			addNodeToOpenList(current.x + offset.x, current.y + offset.y, current)
		}

		if (Statics.supportsDiagonals)
		{
			for (offset in Direction.DiagonalValues)
			{
				addNodeToOpenList(current.x + offset.x, current.y + offset.y, current)
			}
		}

		current.processed = true
	}

	private inline fun isStart(x: Int, y: Int): Boolean
	{
		return x == startx && y == starty
	}

	private inline fun isEnd(x: Int, y: Int): Boolean
	{
		return x == endx && y == endy
	}

	private fun addNodeToOpenList(x: Int, y: Int, parent: Node?)
	{
		if (!isStart(x, y) && !isEnd(x, y))
		{
			for (ix in 0 until actorSize)
			{
				for (iy in 0 until actorSize)
				{
					if (isColliding(x + ix, y + iy))
					{
						return
					}
				}
			}
		}

		val heuristic = Math.abs(x - endx) + Math.abs(y - endy)
		var cost = heuristic + (parent?.cost ?: 0)

		cost += grid[x, y].getInfluence(travelType, self)

		// 3 possible conditions

		val node: Node = nodes[x, y]

		// not added to open list yet, so add it
		if (node.id != currentID)
		{
			node.id = currentID
			node.cost = cost
			node.parent = parent
			openList.add(node, node.cost.toFloat())

			nodes[x, y] = node
		}

		// not yet processed, if lower cost update the values and reposition in list
		else if (!node.processed)
		{
			if (cost < node.cost)
			{
				node.cost = cost
				node.parent = parent

				openList.setValue(node, node.cost.toFloat())
			}
		}

		// processed, if lower cost then update parent and cost
		else
		{
			if (cost < node.cost)
			{
				node.cost = cost
				node.parent = parent
			}
		}
	}

	private inline fun isColliding(x: Int, y: Int): Boolean
	{
		return grid.tryGet(x, y, null)?.getPassable(travelType, self) != true
	}

	fun getPath(startx: Int, starty: Int, endx: Int, endy: Int, findOptimal: Boolean, actorSize: Int, travelType: SpaceSlot, self: Any): Array<Point>?
	{
		currentID++
		openList.clear()

		this.startx = MathUtils.clamp(startx, 0, width - 1)
		this.starty = MathUtils.clamp(starty, 0, height - 1)

		this.endx = MathUtils.clamp(endx, 0, width - 1)
		this.endy = MathUtils.clamp(endy, 0, height - 1)

		this.currentx = this.startx
		this.currenty = this.starty

		this.findOptimal = findOptimal
		this.actorSize = actorSize
		this.travelType = travelType
		this.self = self

		addNodeToOpenList(startx, starty, null)

		while ((findOptimal || !isEnd(currentx, currenty)) && openList.size > 0)
		{
			path()
		}

		if (nodes[endx, endy].id != currentID)
		{
			return null
		}
		else
		{
			path.clear()

			path.add(Point.obtain().set(endx, endy))

			var node = nodes[endx, endy]

			while (node.id == currentID)
			{
				path.add(Point.obtain().set(node.x, node.y))

				if (node.parent == null) break
				node = node.parent!!
			}

			path.reverse()

			return path
		}
	}

	class Node(val x: Int, val y: Int) : BinaryHeap.Node(0f)
	{
		var id: Int = 0
		var cost: Int = 0
		var parent: Node? = null

		var processed = false

		override fun toString(): String
		{
			return "" + cost
		}
	}
}
