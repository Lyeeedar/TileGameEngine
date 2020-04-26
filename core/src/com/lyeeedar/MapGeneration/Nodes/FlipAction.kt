package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Area", colour = "102,110,232")
class FlipAction : AbstractMapGenerationAction()
{
	var onX: Boolean = true

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		if (onX)
		{
			args.area.flipX = !args.area.flipX
		}
		else
		{
			args.area.flipY = !args.area.flipY
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		onX = xmlData.getBoolean("OnX", true)
		afterLoad()
	}
	override val classID: String = "Flip"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}