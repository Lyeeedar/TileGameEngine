package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData
import java.util.*

class SetStateDecorator : AbstractDecorator()
{
	var completed: ExecutionState = ExecutionState.COMPLETED
	var failed: ExecutionState = ExecutionState.FAILED
	var running: ExecutionState = ExecutionState.RUNNING

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		val state = node?.evaluate(entity, world)
		return when(state)
		{
			ExecutionState.COMPLETED -> completed
			ExecutionState.FAILED -> failed
			ExecutionState.RUNNING -> running
			else -> ExecutionState.NONE
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		completed = ExecutionState.valueOf(xmlData.get("Completed", ExecutionState.COMPLETED.toString())!!.toUpperCase(Locale.ENGLISH))
		failed = ExecutionState.valueOf(xmlData.get("Failed", ExecutionState.FAILED.toString())!!.toUpperCase(Locale.ENGLISH))
		running = ExecutionState.valueOf(xmlData.get("Running", ExecutionState.RUNNING.toString())!!.toUpperCase(Locale.ENGLISH))
	}
	override val classID: String = "SetState"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}