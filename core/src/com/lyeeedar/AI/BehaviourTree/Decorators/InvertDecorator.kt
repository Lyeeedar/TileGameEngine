package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData

class InvertDecorator : AbstractDecorator()
{
	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		state = node?.evaluate(entity, world) ?: ExecutionState.FAILED

		return when (state)
		{
			ExecutionState.COMPLETED -> ExecutionState.FAILED
			ExecutionState.FAILED -> ExecutionState.COMPLETED
			else -> ExecutionState.RUNNING
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "Invert"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}