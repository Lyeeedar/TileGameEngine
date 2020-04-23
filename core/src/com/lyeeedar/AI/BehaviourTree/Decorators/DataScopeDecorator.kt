package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.DataGraphNode
import com.lyeeedar.Util.XmlData

class DataScopeDecorator : AbstractDecorator()
{
	init
	{
		data = ObjectMap<String, Any>()
	}

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		return node?.evaluate(entity, world) ?: ExecutionState.FAILED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "DataScope"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}