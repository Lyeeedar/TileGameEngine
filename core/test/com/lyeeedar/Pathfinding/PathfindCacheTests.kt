package com.lyeeedar.Pathfinding

import com.lyeeedar.SpaceSlot
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class PathfindCacheTests
{
	@Test
	fun getCachedResult()
	{
		val gridStr = """
			#####
			#.+.#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val grid = TestTile.createGrid(gridStr)
		val start = grid.first { it.start }
		val end = grid.first { it.end }

		val cache = PathfindCache<TestTile>()

		val path = cache.getPath(grid, start, end, 1, grid, SpaceSlot.ENTITY)

		if (path != null)
		{
			for (p in path)
			{
				grid[p].path = true
			}
		}

		val pathStr = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		assertEquals(pathStr, TestTile.createOutput(grid))

		for (tile in grid)
		{
			tile.path = false
		}
		val path2 = cache.getPath(grid, start, end, 1, grid, SpaceSlot.ENTITY)
		assertEquals(1, cache.reuseCount)

		if (path2 != null)
		{
			for (p in path2)
			{
				grid[p].path = true
			}
		}

		assertEquals(pathStr, TestTile.createOutput(grid))
	}
}