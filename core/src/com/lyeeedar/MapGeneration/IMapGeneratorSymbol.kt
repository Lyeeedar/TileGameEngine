package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Util.XmlData

interface IMapGeneratorSymbol : IPathfindingTile
{
	var char: Char

	fun clear()
	fun write(other: IMapGeneratorSymbol, overwrite: Boolean = false)

	fun isEmpty(): Boolean

	fun copy(): IMapGeneratorSymbol

	fun load(xmlData: XmlData)
	fun evaluateExtends(symbolTable: ObjectMap<Char, IMapGeneratorSymbol>)
}