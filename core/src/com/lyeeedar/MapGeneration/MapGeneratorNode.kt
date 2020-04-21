package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Nodes.AbstractMapGenerationAction
import com.lyeeedar.MapGeneration.Nodes.NodeArguments
import com.lyeeedar.Util.*

@DataClass(colour = "209,209,143")
@DataGraphNode
class MapGeneratorNode : GraphXmlDataClass<MapGeneratorNode>()
{
	val actions: Array<AbstractMapGenerationAction> = Array<AbstractMapGenerationAction>()

	fun execute(generator: MapGenerator, args: NodeArguments)
	{
		for (action in actions)
		{
			action.execute(generator, args)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val actionsEl = xmlData.getChildByName("Actions")
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val obj = AbstractMapGenerationAction.loadPolymorphicClass(el.get("classID"))
				obj.load(el)
				actions.add(obj)
			}
		}
	}
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		for (item in actions)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}