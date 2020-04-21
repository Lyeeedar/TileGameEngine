package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class SetNamedAreaAction : AbstractMapGenerationAction()
{
	lateinit var name: String
	var overwrite: Boolean = false

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val newArea = args.area.copy()

		var areas = generator.namedAreas[name]
		if (areas == null)
		{
			areas = Array()
			generator.namedAreas[name] = areas
		}

		if (overwrite)
		{
			areas.clear()
		}

		areas.add(newArea)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		name = xmlData.get("Name")
		overwrite = xmlData.getBoolean("Overwrite", false)
	}
	override val classID: String = "SetNamedArea"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}