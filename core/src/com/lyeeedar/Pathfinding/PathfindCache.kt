package com.lyeeedar.Pathfinding

import com.badlogic.gdx.utils.Array
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point

class PathfindCache<T: IPathfindingTile>
{
	private var reuseCount = 0
	private var cachedGrid: Array2D<T>? = null
	private var cachedStart: Point? = null
	private var cachedEnd: Point? = null
	private var cachedSize: Int? = null
	private var cachedSelf: Any? = null
	private var cachedTravelType: SpaceSlot? = null

	private var cachedPath: Array<Point>? = null

	private val tempArray = Array<Point>(false, 8)

	fun getPath(grid: Array2D<T>, start: Point, end: Point, size: Int, self: Any, travelType: SpaceSlot): Array<Point>?
	{
		var recalculate = false

		reuseCount++
		if (reuseCount > 5)
		{
			reuseCount = 0
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
				for (i in 0 until path.size)
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

			if (path.last() != end)
			{
				for (i in path.size-1 downTo 0)
				{
					val p = path[i]
					if (p == end)
					{
						endI = i
						break
					}
				}
			}
			else
			{
				endI = path.size-1
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
			cachedGrid = grid
			cachedStart = start
			cachedEnd = end
			cachedSize = size
			cachedSelf = self
			cachedTravelType = travelType

			if (cachedPath != null)
			{
				Point.freeAll(cachedPath!!)
				cachedPath = null
			}

			val astar = AStarPathfind(grid, start.x, start.y, end.x, end.y, false, size, travelType, self)
			cachedPath = astar.path

			if (cachedPath == null)
			{
				cachedPath = BresenhamLine.lineNoDiag(start.x, start.y, end.x, end.y, grid, true, Integer.MAX_VALUE, travelType, self)
			}
		}

		return cachedPath
	}
}