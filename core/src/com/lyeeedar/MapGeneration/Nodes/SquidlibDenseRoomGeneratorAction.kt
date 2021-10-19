package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidgrid.mapping.DenseRoomMapGenerator
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidmath.RNG

@DataClass(category = "Squidlib", colour = "89,255,11")
class SquidlibDenseRoomGeneratorAction : AbstractMapGenerationAction()
{
	var overwrite: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val gen = DenseRoomMapGenerator(args.area.width, args.area.height, RNG(rng))
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
		overwrite = xmlData.getBoolean("Overwrite", true)
	}
	override val classID: String = "SquidlibDenseRoomGenerator"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}