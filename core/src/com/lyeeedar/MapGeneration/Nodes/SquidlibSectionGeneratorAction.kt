package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.SectionDungeonGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG
import java.util.*

class SquidlibSectionGeneratorAction : AbstractMapGenerationAction()
{
	lateinit var tilesetType: TilesetType
	var water: Int = 0
	var lake: Int = 0
	var maze: Int = 0
	var traps: Int = 0
	var doors: Int = 0
	var startChar: Char = ' '
	var endChar: Char = ' '
	var overwrite: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val gen = SectionDungeonGenerator(args.area.width, args.area.height, RNG(generator.ran))
		gen.addWater(SectionDungeonGenerator.ALL, water)
		gen.addLake(lake)
		gen.addMaze(maze)
		gen.addGrass(SectionDungeonGenerator.CAVE, 150) // make all cave cgrass
		gen.addTraps(SectionDungeonGenerator.ALL, traps)
		gen.addDoors(doors, true)
		gen.generate(tilesetType)

		val map = gen.dungeon
		DungeonUtility.closeDoors(map)

		if (map.size != args.area.width || map[0].size != args.area.height) throw Exception("Generator map is the wrong size!")

		for (x in 0 until args.area.width)
		{
			for (y in 0 until args.area.height)
			{
				val char = map[x][y]
				val symbolToWrite = args.symbolTable[char]!!

				val symbol = args.area[x, y] ?: continue
				symbol.write(symbolToWrite, overwrite)
			}
		}

		if (startChar != ' ')
		{
			val symbolToWrite = args.symbolTable[startChar]!!

			val symbol = args.area[gen.stairsUp.x, gen.stairsUp.y]

			if (symbol != null)
			{
				symbol.write(symbolToWrite, overwrite)
			}
		}

		if (endChar != ' ')
		{
			val symbolToWrite = args.symbolTable[endChar]!!

			val symbol = args.area[gen.stairsDown.x, gen.stairsDown.y]

			if (symbol != null)
			{
				symbol.write(symbolToWrite, overwrite)
			}
		}
	}
}