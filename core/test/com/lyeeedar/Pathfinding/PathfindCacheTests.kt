package com.lyeeedar.Pathfinding

import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class PathfindCacheTests
{
	fun updateGrid(grid: Array2D<TestTile>, gridStr: String)
	{
		val newGrid = TestTile.createGrid(gridStr)

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				grid[x, y] = newGrid[x, y]
			}
		}
	}

	fun testCaching(original: String, originalPath: String, updated: String, updatedPath: String, expectedToRecalculate: Boolean)
	{
		val grid = TestTile.createGrid(original)
		var start = grid.first { it.start }
		var end = grid.first { it.end }

		val cache = PathfindCache<TestTile>()

		val path = cache.getPath(grid, start, end, 1, grid, SpaceSlot.ENTITY)
		assertEquals(0, cache.reuseCount)

		if (path != null)
		{
			for (p in path)
			{
				grid[p].path = true
			}
		}

		assertEquals(originalPath, TestTile.createOutput(grid))

		updateGrid(grid, updated)
		start = grid.first { it.start }
		end = grid.first { it.end }

		val path2 = cache.getPath(grid, start, end, 1, grid, SpaceSlot.ENTITY)

		if (expectedToRecalculate)
		{
			assertEquals(0, cache.reuseCount)
		}
		else
		{
			assertEquals(1, cache.reuseCount)
		}

		if (path2 != null)
		{
			for (p in path2)
			{
				grid[p].path = true
			}
		}

		assertEquals(updatedPath, TestTile.createOutput(grid))
	}

	@Test
	fun samePosition()
	{
		val grid = """
			#####
			#.+.#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		testCaching(grid, path, grid, path, false)
	}

	@Test
	fun differentBoth()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#..+#
			#...#
			#..@#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#..p#
			#..p#
			#..p#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, true)
	}

	@Test
	fun differentStart()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#.+.#
			#...#
			#..@#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#.p.#
			#.pp#
			#..p#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, true)
	}

	@Test
	fun startFurther()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#.@.#
			#...#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#...#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, false)
	}

	@Test
	fun targetCloser()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#...#
			#...#
			#.+.#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#...#
			#...#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, false)
	}

	@Test
	fun blockedBehind()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#.+.#
			#...#
			#...#
			#.@.#
			#...#
			#.#.#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#...#
			#.#.#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, false)
	}

	@Test
	fun blockedAfter()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#####
			#####
			#.+.#
			#...#
			#..##
			#.@.#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#####
			#####
			#.p.#
			#.p.#
			#.p##
			#.p.#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, false)
	}

	@Test
	fun blockedInPath()
	{
		val grid1 = """
			#####
			#.+.#
			#...#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path1 = """
			#####
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		val grid2 = """
			#####
			#.+.#
			#.#.#
			#...#
			#...#
			#...#
			#.@.#
			#####
			""".trimIndent()

		val path2 = """
			#####
			#.pp#
			#.#p#
			#.pp#
			#.p.#
			#.p.#
			#.p.#
			#####
		""".trimIndent()

		testCaching(grid1, path1, grid2, path2, true)
	}
}