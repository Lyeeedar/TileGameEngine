package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.Util.XmlData
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.FlowingCaveGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG
import java.util.*

class SquidlibFlowingCaveGeneratorAction : AbstractMapGenerationAction()
{
	lateinit var tilesetType: TilesetType
	var roomChance: Float = 0.0f
	var overwrite: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val gen = FlowingCaveGenerator(args.area.width, args.area.height, tilesetType, RNG(generator.ran))
		gen.generate(tilesetType, roomChance.toDouble())

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