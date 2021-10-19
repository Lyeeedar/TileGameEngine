package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Point", colour = "157,31,189")
class PerPointAction : AbstractMapGenerationAction()
{
	@DataGraphReference
	lateinit var node: MapGeneratorNode

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		for (point in args.area.getAllPoints())
		{
			val newArea = args.area.copy()
			newArea.points.clear()
			newArea.points.add(point)

			val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)

			node.execute(generator, newArgs)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		nodeGUID = xmlData.get("Node")
	}
	override val classID: String = "PerPoint"
	private lateinit var nodeGUID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		node = nodes[nodeGUID]!!
	}
	//endregion
}