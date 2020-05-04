package com.lyeeedar.Pathfinding

import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.StringBuilder

class AStarPathfindTests
{
	@Test
	fun testFunctions()
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

		assertEquals(2, start.x)
		assertEquals(3, start.y)

		assertEquals(2, end.x)
		assertEquals(1, end.y)

		assertEquals(gridStr, TestTile.createOutput(grid))
	}

	fun testPathing(original: String, expected: String, optimal: Boolean)
	{
		val grid = TestTile.createGrid(original)
		val start = grid.first { it.start }
		val end = grid.first { it.end }

		val path = AStarPathfind(grid).getPath(start.x, start.y, end.x, end.y, optimal, 1, SpaceSlot.ENTITY, grid)

		if (path != null)
		{
			for (p in path)
			{
				grid[p].path = true
			}
		}

		assertEquals(expected, TestTile.createOutput(grid))
	}

	@Test
	fun simplePath()
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

		testPathing(grid, path, false)
	}

	@Test
	fun pathAroundWall()
	{
		val grid = """
			########
			#......#
			#.....+#
			#.######
			#.....@#
			########
		""".trimIndent()

		val path = """
			########
			#......#
			#pppppp#
			#p######
			#pppppp#
			########
		""".trimIndent()

		testPathing(grid, path, false)
	}

	@Test
	fun pathBehindObject()
	{
		val grid = """
			........
			....+...
			....#...
			........
			........
			....@...
		""".trimIndent()

		val path = """
			........
			...pp...
			...p#...
			...pp...
			....p...
			....p...
		""".trimIndent()

		testPathing(grid, path, false)
	}

	@Test
	fun influenced()
	{
		val grid = """
			........
			........
			....+...
			....~...
			........
			........
			....@...
		""".trimIndent()

		val path = """
			........
			........
			...pp...
			...p~...
			...pp...
			....p...
			....p...
		""".trimIndent()

		testPathing(grid, path, false)
	}

	@Test
	fun indent()
	{
		val grid = """
			...........
			.....+.....
			...######..
			...#.@..#..
			...#....#..
			...#....#..
			...........
			...........
		""".trimIndent()

		val path = """
			...........
			..pppp.....
			..p######..
			..p#.p..#..
			..p#.p..#..
			..p#pp..#..
			..ppp......
			...........
		""".trimIndent()

		testPathing(grid, path, false)
	}

	@Test
	fun indentInfluence()
	{
		val grid = """
			...........
			.....+.....
			...######..
			...#.@..#..
			...#....#..
			...#....#..
			...~.......
			...~.......
		""".trimIndent()

		val path = """
			...........
			.....ppppp.
			...######p.
			...#.p..#p.
			...#.p..#p.
			...#.ppp#p.
			...~...ppp.
			...~.......
		""".trimIndent()

		testPathing(grid, path, false)
	}

	@Test
	fun noValidPath()
	{
		val grid = """
			.........
			....+....
			.........
			#########
			.........
			....@....
			.........
		""".trimIndent()

		val path = """
			.........
			....+....
			.........
			#########
			.........
			....@....
			.........
		""".trimIndent()

		testPathing(grid, path, true)
	}
}

class TestTile(x: Int, y: Int, val char: Char) : Point(x, y), IPathfindingTile
{
	var isSolid = false
	var start = false
	var end = false
	var path = false
	var influence = false

	init
	{
		if (char == '#')
		{
			isSolid = true
		}
		else if (char == '@')
		{
			start = true
		}
		else if (char == '+')
		{
			end = true
		}
		else if (char == '~')
		{
			influence = true
		}
	}

	fun getFinalChar(): Char
	{
		return when
		{
			path -> 'p'
			start -> '@'
			end -> '+'
			isSolid -> '#'
			influence -> '~'
			else -> '.'
		}
	}

	override fun getPassable(travelType: SpaceSlot, self: Any?): Boolean
	{
		return !isSolid
	}

	override fun getInfluence(travelType: SpaceSlot, self: Any?): Int
	{
		return if (influence) 100 else 0
	}

	companion object
	{
		fun createGrid(gridStr: String): Array2D<TestTile>
		{
			val lines = gridStr.split("\n")
			val firstLineLen = lines[0].count()

			return Array2D<TestTile>(firstLineLen, lines.count()) {x,y -> TestTile(x,y,lines[y][x])}
		}

		fun createOutput(grid: Array2D<TestTile>): String
		{
			val builder = StringBuilder()

			for (y in 0 until grid.height)
			{
				for (x in 0 until grid.width)
				{
					builder.append(grid[x,y].getFinalChar())
				}
				builder.append("\n")
			}

			return builder.toString().trim()
		}
	}
}