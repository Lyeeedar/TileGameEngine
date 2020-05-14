package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.SerpentMapGenerator
import squidpony.squidmath.RNG

@DataClass(category = "Squidlib", colour = "89,255,11")
class SquidlibSerpentMapGeneratorAction : AbstractMapGenerationAction()
{
	var branchingChance: Float = 0.0f
	var symmetrical: Boolean = false

	var numCaveCarvers: Int = 0
	var numBoxRoomCarvers: Int = 0
	var numWalledBoxRoomCarvers: Int = 0
	var numRoundRoomCarvers: Int = 0
	var numWalledRoundRoomCarvers: Int = 0

	var overwrite: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val gen = SerpentMapGenerator(args.area.width, args.area.height, RNG(rng), branchingChance.toDouble(), symmetrical)
		gen.putCaveCarvers(numCaveCarvers)
		gen.putBoxRoomCarvers(numBoxRoomCarvers)
		gen.putWalledBoxRoomCarvers(numWalledBoxRoomCarvers)
		gen.putRoundRoomCarvers(numRoundRoomCarvers)
		gen.putWalledRoundRoomCarvers(numWalledRoundRoomCarvers)
		gen.generate()

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
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		branchingChance = xmlData.getFloat("BranchingChance", 0.0f)
		symmetrical = xmlData.getBoolean("Symmetrical", false)
		numCaveCarvers = xmlData.getInt("NumCaveCarvers", 0)
		numBoxRoomCarvers = xmlData.getInt("NumBoxRoomCarvers", 0)
		numWalledBoxRoomCarvers = xmlData.getInt("NumWalledBoxRoomCarvers", 0)
		numRoundRoomCarvers = xmlData.getInt("NumRoundRoomCarvers", 0)
		numWalledRoundRoomCarvers = xmlData.getInt("NumWalledRoundRoomCarvers", 0)
		overwrite = xmlData.getBoolean("Overwrite", true)
	}
	override val classID: String = "SquidlibSerpentMapGenerator"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}