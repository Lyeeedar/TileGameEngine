package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.DataGraphNode
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

@DataGraphNode
abstract class AbstractDecorator(): AbstractNodeContainer()
{
	@DataGraphReference
	var node: AbstractNodeContainer? = null

	// ----------------------------------------------------------------------
	override fun <T> findData(key: String): T?
	{
		val thisVar = super.findData<T>(key)
		if (thisVar != null)
		{
			return thisVar
		}

		if (node != null)
		{
			val nodeVar = node!!.findData<T>(key)
			if (nodeVar != null)
			{
				return nodeVar
			}
		}

		return null
	}

	// ----------------------------------------------------------------------
	override fun cancel(entity: Entity, world: World)
	{
		node?.cancel(entity, world)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		nodeGUID = xmlData.get("Node", null)
	}
	override val classID: String = "Abstract"
	var nodeGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
		if (!nodeGUID.isNullOrBlank()) node = nodes[nodeGUID]!!
	}
	//endregion
}