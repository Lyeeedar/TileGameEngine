package com.lyeeedar.AI.BehaviourTree.Selectors

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData
import java.util.*

class UntilSelector : AbstractSelector()
{
	lateinit var until: ExecutionState

	//----------------------------------------------------------------------
	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		state = ExecutionState.FAILED

		var i = 0
		while (i < children.size)
		{
			val temp = children.get(i).evaluate(entity, world)
			if (temp == until)
			{
				state = ExecutionState.COMPLETED
				break
			}
			i++
		}
		i++
		while (i < children.size)
		{
			children.get(i).cancel(entity, world)
			i++
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
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		until = ExecutionState.valueOf(xmlData.get("Until").toUpperCase(Locale.ENGLISH))
	}
	override val classID: String = "Until"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}