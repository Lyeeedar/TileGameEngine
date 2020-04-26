package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.*
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader
import com.lyeeedar.Util.copy
import java.util.*

abstract class AbstractMapGenerationAction : GraphXmlDataClass<MapGeneratorNode>()
{
	abstract fun execute(generator: MapGenerator, args: NodeArguments)

	//region generated
	override fun load(xmlData: XmlData)
	{
		afterLoad()
	}
	abstract val classID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
	}
	//endregion
}

class NodeArguments(val area: Area, val variables: ObjectFloatMap<String>, val symbolTable: ObjectMap<Char, IMapGeneratorSymbol>)
{
	fun copy(scopeArea: Boolean = false, scopeVariables: Boolean = false, scopeSymbols: Boolean = false): NodeArguments
	{
		val area = if (scopeArea) this.area.copy() else this.area
		val variables = if (scopeVariables) this.variables.copy() else this.variables
		val symbols = if (scopeSymbols) this.symbolTable.copy() else this.symbolTable

		return NodeArguments(area, variables, symbols)
	}
}