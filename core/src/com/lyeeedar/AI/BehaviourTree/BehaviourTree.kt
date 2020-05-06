package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Components.position
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import java.lang.RuntimeException
import java.util.*
import ktx.collections.set
import squidpony.squidmath.LightRNG

class BehaviourTreeState
{
	lateinit var entity: EntityReference
	lateinit var world: World<*>

	lateinit var rng: LightRNG

	var dataScope = 0

	var lastEvaluationID = 0
	var evaluationID = 0

	fun set(entity: EntityReference, world: World<*>, seed: Long)
	{
		this.entity = entity
		this.world = world
		this.rng = LightRNG(seed)

		dataScope = 0
		lastEvaluationID = 0
		evaluationID = 0

		data.clear()
		variables.clear()
		dynamicVariables.clear()
	}

	inline fun buildKey(key: String, scope: Int, guid: Int): String = "$guid$scope|$key"

	private val data = ObjectMap<String, Any>()
	private val variables = ObjectFloatMap<String>()
	private val dynamicVariables = ObjectMap<String, String>()

	fun <T> getData(key: String, guid: Int, fallback: T? = null): T?
	{
		var currentScope = dataScope
		while (currentScope >= 0)
		{
			val dataKey = buildKey(key, currentScope, guid)
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
		if (value is Entity) throw RuntimeException("Use entityreference!")

		val dataKey = buildKey(key, dataScope, guid)
		data[dataKey] = value

		if (guid == 0)
		{
			variables[key.toLowerCase(Locale.ENGLISH)] = when (value)
			{
				is Float -> value
				is Int -> value.toFloat()
				is Boolean -> if (value) 1.0f else 0.0f
				is EntityReference -> if (value.isValid()) 1.0f else 0.0f
				else -> 1.0f
			}

			if (value is Entity || value is Point)
			{
				dynamicVariables[dataKey] = key.toLowerCase(Locale.ENGLISH)
			}
		}
	}

	fun removeData(key: String, guid: Int)
	{
		val dataKey = buildKey(key, dataScope, guid)
		val data = data.remove(dataKey)

		if (guid == 0)
		{
			variables.remove(key.toLowerCase(Locale.ENGLISH), 0f)
			if (data is EntityReference || data is Point)
			{
				variables.remove("${key.toLowerCase(Locale.ENGLISH)}.dist", 0f)
				dynamicVariables.remove(dataKey)
			}
		}
	}

	fun getVariables(): ObjectFloatMap<String>
	{
		// update dynamic
		val srcPos = entity.entity.position()!!
		for (dynamic in dynamicVariables)
		{
			val value = data[dynamic.key]
			val key = dynamic.value

			if (value is EntityReference)
			{
				if (value.isValid())
				{
					val epos = value.entity.position()
					if (epos != null)
					{
						val dist = srcPos.position.dist(epos.position)
						variables["$key.dist"] = dist.toFloat()
					}
				}
				else
				{
					variables[key] = 0f
					variables["$key.dist"] = 0f
				}
			}
			else if (value is Point)
			{
				val dist = srcPos.position.dist(value)
				variables["$key.dist"] = dist.toFloat()
			}
			else
			{
				throw RuntimeException("Unhandled dynamic variable type '${value.javaClass.name}'")
			}
		}

		return variables
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
		if (!state.entity.isValid()) return

		state.lastEvaluationID = state.evaluationID
		state.evaluationID++

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
				val obj = XmlDataClassLoader.loadAbstractBehaviourNode(el.get("classID", el.name)!!)
				obj.load(el)
				val guid = el.getAttribute("GUID")
				nodeMap[guid] = obj
			}
		}
		rootGUID = xmlData.get("Root")
		resolve(nodeMap)
		afterLoad()
	}
	private lateinit var rootGUID: String
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