package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.SerpentMapGenerator
import squidpony.squidmath.RNG

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
		val gen = SerpentMapGenerator(args.area.width, args.area.height, RNG(generator.ran), branchingChance.toDouble(), symmetrical)
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
}