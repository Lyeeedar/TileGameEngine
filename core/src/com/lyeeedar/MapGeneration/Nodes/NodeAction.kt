package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData

class NodeAction : AbstractMapGenerationAction()
{
	@DataGraphReference
	lateinit var child: MapGeneratorNode

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		child.execute(args)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		childGUID = xmlData.get("Child")
	}
	override val classID: String = "Node"
	lateinit var childGUID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		child = nodes[childGUID]!!
	}
	//endregion
}