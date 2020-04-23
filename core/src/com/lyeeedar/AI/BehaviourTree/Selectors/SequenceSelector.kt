package com.lyeeedar.AI.BehaviourTree.Selectors

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData

class SequenceSelector : AbstractSelector()
{
	//----------------------------------------------------------------------
	var reset: Boolean = false

	//region non-data
	var i: Int = 0
	//endregion

	//----------------------------------------------------------------------
	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		state = ExecutionState.COMPLETED

		while (i < children.size)
		{
			val temp = children.get(i).evaluate(entity, world)
			if (temp != ExecutionState.COMPLETED)
			{
				state = temp
				break
			}
			i++
		}

		if (state != ExecutionState.RUNNING)
		{
			i = 0
			while (i < children.size)
			{
				children.get(i).cancel(entity, world)
				i++
			}
			i = 0
		}

		if (reset)
		{
			i = 0
		}

		return state
	}

	//----------------------------------------------------------------------
	override fun cancel(entity: Entity, world: World)
	{
		for (i in 0 until children.size)
		{
			children.get(i).cancel(entity, world)
		}
		i = 0
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		reset = xmlData.getBoolean("Reset", false)
	}
	override val classID: String = "Sequence"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}