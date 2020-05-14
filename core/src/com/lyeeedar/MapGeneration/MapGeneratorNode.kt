package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Nodes.AbstractMapGenerationAction
import com.lyeeedar.MapGeneration.Nodes.NodeArguments
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

@DataClass(colour = "209,209,143")
@DataClassCollection
@DataGraphNode
class MapGeneratorNode : GraphXmlDataClass<MapGeneratorNode>()
{
	val actions: Array<AbstractMapGenerationAction> = Array<AbstractMapGenerationAction>()

	fun execute(generator: MapGenerator, args: NodeArguments)
	{
		if (Statics.debug) generator.debugExecuteNode.invoke(this, args)

		for (action in actions)
		{
			action.execute(generator, args)

			if (Statics.debug) generator.debugExecuteAction.invoke(this, action, args)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val actionsEl = xmlData
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val objactions: AbstractMapGenerationAction
				val objactionsEl = el
				objactions = XmlDataClassLoader.loadAbstractMapGenerationAction(objactionsEl.get("classID", objactionsEl.name)!!)
				objactions.load(objactionsEl)
				actions.add(objactions)
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