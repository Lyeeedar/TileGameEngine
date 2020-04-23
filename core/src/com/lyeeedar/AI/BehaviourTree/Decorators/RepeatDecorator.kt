package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData
import java.util.*

class RepeatDecorator : AbstractDecorator()
{
	var until: ExecutionState? = null
	var repeats: Int = 1

	//region non-data
	var i: Int = 0
	//endregion

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		val retState = node?.evaluate(entity, world)

		if (until != null)
		{
			if (retState == until)
			{
				state = ExecutionState.COMPLETED
				return state
			}
		}

		i++

		if (i == repeats)
		{
			state = ExecutionState.COMPLETED
			return state
		}

		state = ExecutionState.RUNNING
		return state
	}

	override fun cancel(entity: Entity, world: World)
	{
		super.cancel(entity, world)
		i = 0
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		until = ExecutionState.valueOf(xmlData.get("Until", null.toString())!!.toUpperCase(Locale.ENGLISH))
		repeats = xmlData.getInt("Repeats", 1)
	}
	override val classID: String = "Repeat"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}