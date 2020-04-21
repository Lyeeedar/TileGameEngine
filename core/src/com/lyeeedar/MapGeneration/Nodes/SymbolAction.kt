package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataXml
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Setup", colour = "227,198,16")
class SymbolAction : AbstractMapGenerationAction()
{
	@DataXml(actualClass = "Symbol")
	lateinit var symbolDef: XmlData

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val symbol = Symbol()
		symbol.load(symbolDef)
		args.symbolTable[symbol.char] = symbol
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		symbolDef = xmlData.getChildByName("SymbolDef")!!
	}
	override val classID: String = "Symbol"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}