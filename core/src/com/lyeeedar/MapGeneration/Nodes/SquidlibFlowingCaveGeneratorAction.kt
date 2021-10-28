package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.FlowingCaveGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.RNG

@DataClass(category = "Squidlib", colour = "89,255,11")
class SquidlibFlowingCaveGeneratorAction : AbstractMapGenerationAction()
{
	lateinit var tilesetType: TilesetType
	var roomChance: Float = 0.0f
	var overwrite: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val gen = FlowingCaveGenerator(args.area.width, args.area.height, tilesetType, RNG(rng))
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

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		tilesetType = TilesetType.valueOf(xmlData.get("TilesetType").uppercase(Locale.ENGLISH))
		roomChance = xmlData.getFloat("RoomChance", 0.0f)
		overwrite = xmlData.getBoolean("Overwrite", true)
	}
	override val classID: String = "SquidlibFlowingCaveGenerator"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}