package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Write", colour = "214,252,1")
class FillAction : AbstractMapGenerationAction()
{
	var char: Char = ' '
	var overwrite: Boolean = false

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val symbolToWrite = args.symbolTable[char]

		for (pos in args.area.getAllPoints())
		{
			val symbol = args.area[pos.x - args.area.x, pos.y - args.area.y] ?: continue

			symbol.write(symbolToWrite, overwrite)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		char = xmlData.get("Char", " ")!![0]
		overwrite = xmlData.getBoolean("Overwrite", false)
		afterLoad()
	}
	override val classID: String = "Fill"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}