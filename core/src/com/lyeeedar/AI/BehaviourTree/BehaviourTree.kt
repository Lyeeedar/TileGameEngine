package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.Components.Entity
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataFile(colour="121,252,218", icon="Sprites/Icons/CardIcon.png")
class BehaviourTree : GraphXmlDataClass<AbstractNodeContainer>()
{
	@DataGraphReference
	lateinit var root: AbstractNodeContainer

	@DataGraphNodes
	val nodeMap: ObjectMap<String, AbstractNodeContainer> = ObjectMap<String, AbstractNodeContainer>()

	fun setData(key: String, value: Any?) { root.setData(key, value) }

	fun update(e: Entity, world: World)
	{
		root.evaluate(e, world)
	}

	fun cancel(e: Entity, world: World)
	{
		root.cancel(e, world)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		rootGUID = xmlData.get("Root")
		val nodeMapEl = xmlData.getChildByName("NodeMap")
		if (nodeMapEl != null)
		{
			for (el in nodeMapEl.children)
			{
				val obj =  XmlDataClassLoader.loadAbstractNodeContainer(el.get("classID"))
				obj.load(el)
				val guid = el.getAttribute("GUID")
				nodeMap[guid] = obj
			}
		}
		resolve(nodeMap)
	}
	lateinit var rootGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		root = nodes[rootGUID]!!
		for (item in nodeMap.values())
		{
			item.resolve(nodes)
		}
	}
	//endregion
}