package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Components.Entity
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

	private val data = ObjectMap<String, Any>()
	fun <T> getData(key: String, guid: Int, fallback: T? = null): T?
	{
		return data[key+guid+dataScope] as? T ?: fallback
	}
	fun setData(key: String, guid: Int, value: Any)
	{
		data[key+guid+dataScope] = value
	}
	fun removeData(key: String, guid: Int)
	{
		data.remove(key+guid+dataScope)
	}
}

@DataFile(colour="121,252,218", icon="Sprites/Icons/CardIcon.png")
class BehaviourTree : GraphXmlDataClass<AbstractBehaviourNode>()
{
	@DataGraphNodes
	val nodeMap: ObjectMap<String, AbstractBehaviourNode> = ObjectMap<String, AbstractBehaviourNode>()

	@DataGraphReference
	lateinit var root: AbstractBehaviourNode

	fun afterLoad()
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