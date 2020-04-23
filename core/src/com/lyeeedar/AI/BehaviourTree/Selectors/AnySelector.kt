package com.lyeeedar.AI.BehaviourTree.Selectors

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData

class AnySelector : AbstractSelector()
{
	var reset: Boolean = false

	//region non-data
	val runningList: com.badlogic.gdx.utils.Array<Int> = com.badlogic.gdx.utils.Array<Int>(1)
	//endregion

	//----------------------------------------------------------------------
	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		state = ExecutionState.FAILED

		if (runningList.size > 0)
		{
			val itr = runningList.iterator()
			while (itr.hasNext())
			{
				val temp = children.get(itr.next()).evaluate(entity, world)

				if (state != ExecutionState.RUNNING && temp == ExecutionState.COMPLETED)
				{
					state = temp
					itr.remove()
				}
				else if (temp == ExecutionState.RUNNING)
				{
					state = temp
				}
				else
				{
					itr.remove()
				}
			}
		}
		else
		{
			for (i in 0 until children.size)
			{
				val temp = children.get(i).evaluate(entity, world)
				if (state != ExecutionState.RUNNING && temp == ExecutionState.COMPLETED)
				{
					state = temp
				}
				else if (temp == ExecutionState.RUNNING)
				{
					state = temp
					runningList.add(i)
				}
			}
		}

		if (reset)
		{
			runningList.clear()
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

		runningList.clear()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		reset = xmlData.getBoolean("Reset", false)
	}
	override val classID: String = "Any"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}