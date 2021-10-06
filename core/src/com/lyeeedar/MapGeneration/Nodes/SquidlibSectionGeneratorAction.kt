package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.SectionDungeonGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG
import java.util.*

@DataClass(category = "Squidlib", colour = "89,255,11")
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
		val gen = SectionDungeonGenerator(args.area.width, args.area.height, RNG(rng))
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

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		tilesetType = TilesetType.valueOf(xmlData.get("TilesetType").toUpperCase(Locale.ENGLISH))
		water = xmlData.getInt("Water", 0)
		lake = xmlData.getInt("Lake", 0)
		maze = xmlData.getInt("Maze", 0)
		traps = xmlData.getInt("Traps", 0)
		doors = xmlData.getInt("Doors", 0)
		startChar = xmlData.get("StartChar", " ")!![0]
		endChar = xmlData.get("EndChar", " ")!![0]
		overwrite = xmlData.getBoolean("Overwrite", true)
	}
	override val classID: String = "SquidlibSectionGenerator"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}