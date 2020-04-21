package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData

@DataClass(category = "Other", colour = "255,0,0")
class DeferAction : AbstractMapGenerationAction()
{
	@DataGraphReference
	lateinit var child: MapGeneratorNode

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		generator.deferredNodes.add(DeferredNode(child, args.copy(true, true, true)))
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		childGUID = xmlData.get("Child")
	}
	override val classID: String = "Defer"
	lateinit var childGUID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		child = nodes[childGUID]!!
	}
	//endregion
}

class DeferredNode(val node: MapGeneratorNode, val args: NodeArguments)