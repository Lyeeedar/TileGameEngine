package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.position
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set
import squidpony.squidmath.LightRNG

class BehaviourTreeState
{
	lateinit var entity: Entity
	lateinit var world: World

	lateinit var rng: LightRNG

	var dataScope = 0

	var lastEvaluationID = 0
	var evaluationID = 0

	fun set(entity: Entity, world: World, seed: Long)
	{
		this.entity = entity
		this.world = world
		this.rng = LightRNG(seed)

		dataScope = 0
		lastEvaluationID = 0
		evaluationID = 0

		data.clear()
	}

	private val data = ObjectMap<String, Any>()
	fun <T> getData(key: String, guid: Int, fallback: T? = null): T?
	{
		var currentScope = dataScope
		while (currentScope >= 0)
		{
			val dataKey = "$guid$currentScope$key"
			val value = data[dataKey] as? T

			if (value != null)
			{
				return value
			}

			currentScope--
		}

		return fallback
	}
	fun setData(key: String, guid: Int, value: Any)
	{
		val dataKey = "$guid$dataScope$key"
		data[dataKey] = value

		if (value is Entity || value is Point)
		{
			updateResolvedVariables()
		}
	}
	fun removeData(key: String, guid: Int)
	{
		val dataKey = "$guid$dataScope$key"
		data.remove(dataKey)
	}

	private val resolvedVariables = ObjectFloatMap<String>()
	fun updateResolvedVariables()
	{
		val srcPos = entity.position()!!

		resolvedVariables.clear()
		for (entry in data)
		{
			val value = entry.value
			if (value is Entity)
			{
				val epos = value.position()
				if (epos != null)
				{
					val dist = srcPos.position.taxiDist(epos.position)
					resolvedVariables[entry.key+".dist"] = dist.toFloat()
				}
			}
			else if (value is Point)
			{
				val dist = srcPos.position.taxiDist(value)
				resolvedVariables[entry.key+".dist"] = dist.toFloat()
			}
		}
	}

	val map = ObjectFloatMap<String>()
	fun getVariables(): ObjectFloatMap<String>
	{
		map.clear()
		map.putAll(resolvedVariables)

		for (entry in data)
		{
			val value = entry.value
			map[entry.key] = when(value)
			{
				is Float -> value
				is Int -> value.toFloat()
				is Boolean -> if (value) 1.0f else 0.0f
				else -> 1.0f
			}
		}

		return map
	}
}

@DataFile(colour="121,252,218", icon="Sprites/Icons/CardIcon.png")
class BehaviourTree : GraphXmlDataClass<AbstractBehaviourNode>()
{
	@DataGraphNodes
	val nodeMap: ObjectMap<String, AbstractBehaviourNode> = ObjectMap<String, AbstractBehaviourNode>()

	@DataGraphReference
	lateinit var root: AbstractBehaviourNode

	fun evaluate(state: BehaviourTreeState)
	{
		state.lastEvaluationID = state.evaluationID
		state.evaluationID++
		state.updateResolvedVariables()

		root.evaluate(state)
	}

	override fun afterLoad()
	{
		var i = 0
		for (node in nodeMap.values())
		{
			node.dataGuid = i++
			for (action in node.actions)
			{
				action.dataGuid = i++
			}
		}
	}

	companion object
	{
		private val loaded = ObjectMap<String, BehaviourTree>()

		fun load(path: String): BehaviourTree
		{
			val existing = loaded[path]
			if (existing != null) return existing

			val xml = getXml(path)

			val tree = BehaviourTree()
			tree.load(xml)

			loaded[path] = tree
			return tree
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val nodeMapEl = xmlData.getChildByName("NodeMap")
		if (nodeMapEl != null)
		{
			for (el in nodeMapEl.children)
			{
				val obj =  XmlDataClassLoader.loadAbstractBehaviourNode(el.get("classID"))
				obj.load(el)
				val guid = el.getAttribute("GUID")
				nodeMap[guid] = obj
			}
		}
		rootGUID = xmlData.get("Root")
		resolve(nodeMap)
		afterLoad()
	}
	lateinit var rootGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		for (item in nodeMap.values())
		{
			item.resolve(nodes)
		}
		root = nodes[rootGUID]!!
	}
	//endregion
}

enum class EvaluationState
{
	COMPLETED,
	RUNNING,
	FAILED
}