package com.lyeeedar.Pathfinding

import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point

class Pathfinder<T: IPathfindingTile>(private val grid: Array2D<T>, private val startx: Int, private val starty: Int, private val endx: Int, private val endy: Int, private val size: Int, private val self: Any)
{
	fun getPath(travelType: SpaceSlot): com.badlogic.gdx.utils.Array<Point>?
	{
		val astar = AStarPathfind(grid)
		var path: com.badlogic.gdx.utils.Array<Point>? = astar.getPath(startx, starty, endx, endy, false, size, travelType, self)

		if (path == null)
		{
			path = BresenhamLine.lineNoDiag(startx, starty, endx, endy, grid, true, travelType, self)
		}

		return path
	}
}
