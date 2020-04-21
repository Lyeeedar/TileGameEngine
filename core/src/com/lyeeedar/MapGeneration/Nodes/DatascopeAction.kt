package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData

@DataClass(category = "Other", colour = "201,80,91")
class DatascopeAction : AbstractMapGenerationAction()
{
	var scopeVariables: Boolean = true
	var scopeSymbols: Boolean = true
	var scopeArea: Boolean = true

	@DataGraphReference
	lateinit var child: MapGeneratorNode

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val cpy = args.copy(scopeArea, scopeVariables, scopeSymbols)
		child.execute(generator, cpy)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		scopeVariables = xmlData.getBoolean("ScopeVariables", true)
		scopeSymbols = xmlData.getBoolean("ScopeSymbols", true)
		scopeArea = xmlData.getBoolean("ScopeArea", true)
		childGUID = xmlData.get("Child")
	}
	override val classID: String = "Datascope"
	lateinit var childGUID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		child = nodes[childGUID]!!
	}
	//endregion
}