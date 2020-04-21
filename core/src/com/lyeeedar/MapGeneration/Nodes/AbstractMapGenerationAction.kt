package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.copy
import java.util.*

abstract class AbstractMapGenerationAction : GraphXmlDataClass<MapGeneratorNode>()
{
	abstract fun execute(generator: MapGenerator, args: NodeArguments)

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
	}

	companion object
	{
		fun loadPolymorphicClass(classID: String): AbstractMapGenerationAction
		{
			return when (classID)
			{
				"ChambersGenerator" -> ChambersGeneratorAction()
				"Condition" -> ConditionAction()
				"DefineVariable" -> DefineVariableAction()
				"Fill" -> FillAction()
				"Filter" -> FilterAction()
				"FindRooms" -> FindRoomsAction()
				"Flip" -> FlipAction()
				"PerPoint" -> PerPointAction()
				"Repeat" -> RepeatAction()
				"Scale" -> ScaleAction()
				"SelectNamedArea" -> SelectNamedAreaAction()
				"Split" -> SplitAction()
				"SquidlibDungeonGenerator" -> SquidlibDungeonGeneratorAction()
				"SquidlibLanesMapGenerator" -> SquidlibLanesMapGeneratorAction()
				"SquidlibOrganicMapGenerator" -> SquidlibOrganicMapGeneratorAction()
				"SquidlibSerpentMapGenerator" -> SquidlibSerpentMapGeneratorAction()
				"SquidlibSymmetryGenerator" -> SquidlibSymmetryGeneratorAction()
				"Symbol" -> SymbolAction()
				"Translate" -> TranslateAction()
				"SquidlibDenseRoomGenerator" -> SquidlibDenseRoomGeneratorAction()
				"Node" -> NodeAction()
				"Divide" -> DivideAction()
				"ConnectRooms" -> ConnectRoomsAction()
				"Defer" -> DeferAction()
				"SquidlibFlowingCaveGenerator" -> SquidlibFlowingCaveGeneratorAction()
				"Take" -> TakeAction()
				"Datascope" -> DatascopeAction()
				"Rotate" -> RotateAction()
				"SetNamedArea" -> SetNamedAreaAction()
				"SquidlibSectionGenerator" -> SquidlibSectionGeneratorAction()
				else -> throw RuntimeException("Unknown classID '$classID' for AbstractMapGenerationAction!")
			}
		}
	}
	//endregion
}

class NodeArguments(val area: Area, val variables: ObjectFloatMap<String>, val symbolTable: ObjectMap<Char, Symbol>)
{
	fun copy(scopeArea: Boolean = false, scopeVariables: Boolean = false, scopeSymbols: Boolean = false): NodeArguments
	{
		val area = if (scopeArea) this.area.copy() else this.area
		val variables = if (scopeVariables) this.variables.copy() else this.variables
		val symbols = if (scopeSymbols) this.symbolTable.copy() else this.symbolTable

		return NodeArguments(area, variables, symbols)
	}
}