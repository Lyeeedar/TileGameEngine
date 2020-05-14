package com.lyeeedar.MapGeneration

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Nodes.AbstractMapGenerationAction
import com.lyeeedar.MapGeneration.Nodes.DeferredNode
import com.lyeeedar.MapGeneration.Nodes.NodeArguments
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidmath.LightRNG

@DataFile(colour = "201,200,153", icon="Sprites/Icons/map.png")
class MapGenerator : GraphXmlDataClass<MapGeneratorNode>()
{
	var baseSize: Point = Point(0, 0)

	@DataGraphNodes
	val nodeMap: ObjectMap<String, MapGeneratorNode> = ObjectMap<String, MapGeneratorNode>()

	@DataGraphReference
	lateinit var root: MapGeneratorNode

	//region non-data
	val deferredNodes = Array<DeferredNode>()
	val namedAreas = ObjectMap<String, Array<Area>>()
	private val executingArray = Array<DeferredNode>()
	val debugExecuteNode = Event2Arg<MapGeneratorNode, NodeArguments>()
	val debugExecuteAction = Event3Arg<MapGeneratorNode, AbstractMapGenerationAction, NodeArguments>()
	lateinit var rootArea: Area
	//endregion

	fun execute(seed: Long, createSymbolFunc: (Int,Int)->IMapGeneratorSymbol): Array2D<IMapGeneratorSymbol>
	{
		deferredNodes.clear()
		namedAreas.clear()
		executingArray.clear()

		for (nodeEntry in nodeMap)
		{
			val node = nodeEntry.value
			var nodeSeed = (nodeEntry.key + seed).hashCode().toLong()

			for (action in node.actions)
			{
				action.rng = LightRNG(nodeSeed++)
			}
		}

		val grid = Array2D<IMapGeneratorSymbol>(baseSize.x, baseSize.y) { x,y -> createSymbolFunc(x,y) }
		val area = Area(baseSize.x, baseSize.y, grid)
		val args = NodeArguments(area, ObjectFloatMap(), ObjectMap())

		rootArea = area

		deferredNodes.add(DeferredNode(root, args))
		while (deferredNodes.size > 0)
		{
			executingArray.clear()
			executingArray.addAll(deferredNodes)
			deferredNodes.clear()

			for (i in 0 until executingArray.size)
			{
				val node = executingArray[i]
				node.node.execute(this, node.args)
			}
		}

		if (Statics.debug)
		{
			for (y in 0 until baseSize.y)
			{
				for (x in 0 until baseSize.x)
				{
					print(grid[x, y].char)
				}
				print("\n")
			}
		}

		return grid
	}

	companion object
	{
		fun load(path: String): MapGenerator
		{
			val xml = getXml(path)

			val generator = MapGenerator()
			generator.load(xml)

			return generator
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val baseSizeRaw = xmlData.get("BaseSize", "0, 0")!!.split(',')
		baseSize = Point(baseSizeRaw[0].trim().toInt(), baseSizeRaw[1].trim().toInt())
		val nodeMapEl = xmlData.getChildByName("NodeMap")
		if (nodeMapEl != null)
		{
			for (el in nodeMapEl.children)
			{
				val obj = MapGeneratorNode()
				obj.load(el)
				val guid = el.getAttribute("GUID")
				nodeMap[guid] = obj
			}
		}
		rootGUID = xmlData.get("Root")
		resolve(nodeMap)
	}
	private lateinit var rootGUID: String
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		for (item in nodeMap.values())
		{
			item.resolve(nodes)
		}
		root = nodes[rootGUID]!!
	}
	//endregion
}