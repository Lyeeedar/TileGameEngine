package com.lyeeedar.Pathfinding

import com.badlogic.gdx.utils.Array
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point

class PathfindCache<T: IPathfindingTile>(val maxUses: Int = 10)
{
	internal var reuseCount = 0
	private var cachedGrid: Array2D<T>? = null
	private var cachedStart: Point? = null
	private var cachedEnd: Point? = null
	private var cachedSize: Int? = null
	private var cachedSelf: Any? = null
	private var cachedTravelType: SpaceSlot? = null

	private var pathfinder: AStarPathfind<T>? = null
	private var cachedPath: Array<Point>? = null

	private val tempArray = Array<Point>(false, 8)

	fun invalidatePath()
	{
		if (cachedPath != null)
		{
			Point.freeAll(cachedPath!!)
			cachedPath = null
		}
	}

	fun getPath(grid: Array2D<T>, start: Point, end: Point, size: Int, self: Any, travelType: SpaceSlot, forceRefresh: Boolean = false): Array<Point>?
	{
		if (grid.tryGet(start, null) == null)
		{
			throw RuntimeException("Start is an invalid position: $start")
		}
		else if (grid.tryGet(end, null) == null)
		{
			throw RuntimeException("End is an invalid position: $end")
		}

		var recalculate = forceRefresh

		reuseCount++
		if (reuseCount >= maxUses)
		{
			recalculate = true
		}

		// check if needs updating
		if (cachedPath == null) recalculate = true
		if (grid != cachedGrid || size != cachedSize || travelType != cachedTravelType || self != cachedSelf) recalculate = true
		if (!recalculate)
		{
			val path = cachedPath!!
			// check if path still available
			var startI = -1
			var endI = -1

			if (path.first() != start)
			{
				for (i in 1 until path.size-1)
				{
					val p = path[i]
					if (p == start)
					{
						startI = i
						break
					}
				}
			}
			else
			{
				startI = 0
			}

			if (startI != -1)
			{
				if (path.last() != end)
				{
					if (startI < path.size - 1 && path.size > 3)
					{
						for (i in path.size - 2 downTo startI)
						{
							val p = path[i]
							if (p == end)
							{
								endI = i
								break
							}
						}
					}
				}
				else
				{
					endI = path.size - 1
				}
			}

			if (startI == -1 || endI == -1)
			{
				recalculate = true
			}
			else
			{
				outer@for (i in startI until endI+1)
				{
					val p = path[i]

					for (x in 0 until size)
					{
						for (y in 0 until size)
						{
							val tile = grid.tryGet(p, x, y, null)
							if (tile == null || !tile.getPassable(travelType, self))
							{
								recalculate = true
								break@outer
							}
						}
					}
				}

				if (!recalculate)
				{
					if (startI == 0 && endI == path.size-1)
					{
						return cachedPath
					}
					else
					{
						tempArray.clear()
						for (i in startI until endI+1)
						{
							tempArray.add(path[i])
						}
						return tempArray
					}
				}
			}
		}

		// recalculate if neccesssary
		if (recalculate)
		{
			reuseCount = 0
			cachedGrid = grid
			cachedStart = start
			cachedEnd = end
			cachedSize = size
			cachedSelf = self
			cachedTravelType = travelType

			invalidatePath()

			if (pathfinder == null)
			{
				pathfinder = AStarPathfind(grid)
			}
			cachedPath = pathfinder!!.getPath(start.x, start.y, end.x, end.y, false, size, travelType, self)

			if (cachedPath == null)
			{
				cachedPath = BresenhamLine.lineNoDiag(start.x, start.y, end.x, end.y, grid, true, travelType, self)
			}
		}

		return cachedPath
	}
}