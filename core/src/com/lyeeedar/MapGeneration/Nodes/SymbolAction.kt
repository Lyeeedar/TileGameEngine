package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class SymbolAction : AbstractMapGenerationAction()
{
	lateinit var symbolDef: XmlData

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val symbol = Symbol.load(symbolDef, args.symbolTable)
		args.symbolTable[symbol.char] = symbol
	}
}