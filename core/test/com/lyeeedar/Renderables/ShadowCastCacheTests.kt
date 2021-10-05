package com.lyeeedar.Renderables

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.PointRect
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.StringBuilder

class ShadowCastCacheTests
{
	fun createCollisionGrid(gridStr: String): Pair<Point, Array2D<Boolean>>
	{
		val lines = gridStr.split("\n")
		val firstLineLen = lines[0].count()

		val point = Point()
		val grid = Array2D<Boolean>(firstLineLen, lines.count()) { x, y ->
			val c = lines[y][x]

			if (c == '@') {
				point.set(x, y)
			}

			c == '#'
		}

		return Pair(point, grid)
	}

	fun castToString(points: Array<Point>, grid: Array2D<Boolean>): String
	{
		val visible = points.toSet()

		val builder = StringBuilder()

		for (y in 0 until grid.height)
		{
			for (x in 0 until grid.width)
			{
				val point = Point(x, y)
				val c: Char
				if (visible.contains(point))
				{
					if (grid[x, y])
					{
						c = '#'
					}
					else
					{
						c = '.'
					}
				}
				else
				{
					c = '-'
				}

				builder.append(c)
			}

			builder.append("\n")
		}

		return builder.toString().trim()
	}

	fun regionsToString(regions: Array<PointRect>, grid: Array2D<Boolean>): String
	{
		val builder = StringBuilder()

		for (y in 0 until grid.height)
		{
			for (x in 0 until grid.width)
			{
				val point = Point(x, y)
				var c: Char = if (grid[x, y]) '#' else '.'

				for (i in 0 until regions.size)
				{
					val region = regions[i]
					if (region.contains(point))
					{
						c = i.toString()[0]
						break
					}
				}

				builder.append(c)
			}

			builder.append("\n")
		}

		return builder.toString().trim()
	}

	fun testVisibility(original: String, expected: String)
	{
		val grid = createCollisionGrid(original)

		val cache = ShadowCastCache()
		val cast = cache.getShadowCast(grid.first.x, grid.first.y, 5, grid.second)

		val asStr = castToString(cast, grid.second)

		assertEquals(expected, asStr)
	}

	fun testRegions(original: String, expected: String)
	{
		val grid = createCollisionGrid(original)

		val cache = ShadowCastCache()
		cache.getShadowCast(grid.first.x, grid.first.y, 5, grid.second)
		val regions = cache.getOpaqueRegions()

		val asStr = regionsToString(regions, grid.second)

		assertEquals(expected, asStr)
	}

	@Test
	fun noWalls()
	{
		testVisibility("""
			.......
			.......
			.......
			...@...
			.......
			.......
			.......
		""".trimIndent(),"""
			.......
			.......
			.......
			.......
			.......
			.......
			.......
		""".trimIndent())
	}

	@Test
	fun inBoxSideOpen()
	{
		testVisibility("""
			.......
			.......
			..###..
			..#@#..
			..#.#..
			.......
			.......
		""".trimIndent(),"""
			-------
			-------
			--###--
			--#.#--
			--#.#--
			--...--
			--...--
		""".trimIndent())
	}

	@Test
	fun inBoxSurrounded()
	{
		testVisibility("""
			.......
			.......
			..###..
			..#@#..
			..###..
			.......
			.......
		""".trimIndent(),"""
			-------
			-------
			--###--
			--#.#--
			--###--
			-------
			-------
		""".trimIndent())
	}

	@Test
	fun regionNoWalls()
	{
		testRegions("""
			.......
			.......
			.......
			...@...
			.......
			.......
			.......
		""".trimIndent(),"""
			.......
			.......
			.......
			.......
			.......
			.......
			.......
		""".trimIndent())
	}

	@Test
	fun regionInBoxSurrounded()
	{
		testRegions("""
			.......
			.......
			..###..
			..#@#..
			..###..
			.......
			.......
		""".trimIndent(),"""
			.......
			.......
			..221..
			..3.1..
			..000..
			.......
			.......
		""".trimIndent())
	}

	@Test
	fun regionInBoxSideOpen()
	{
		testRegions("""
			.......
			.......
			..###..
			..#@#..
			..#.#..
			.......
			.......
		""".trimIndent(),"""
			.......
			.......
			..021..
			..0.1..
			..0.1..
			.......
			.......
		""".trimIndent())
	}
}