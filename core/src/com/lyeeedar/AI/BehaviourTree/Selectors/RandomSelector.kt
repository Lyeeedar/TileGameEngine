package com.lyeeedar.AI.BehaviourTree.Selectors

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.removeRandom

class RandomSelector : AbstractSelector()
{
	//----------------------------------------------------------------------
	//region non-data
	val numList: com.badlogic.gdx.utils.Array<Int> = com.badlogic.gdx.utils.Array<Int>(false, 16);
	var i: Int = -1
	//endregion

	//----------------------------------------------------------------------
	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		state = ExecutionState.FAILED

		if (i == -1)
		{
			numList.clear()
			for (n in 0 until children.size) { numList.add(n); }

			while (state == ExecutionState.FAILED && numList.size > 0)
			{
				i = numList.removeRandom(ran)
				state = children.get(i).evaluate(entity, world)
			}
		}
		else
		{
			state = children.get(i).evaluate(entity, world)
		}

		if (state != ExecutionState.RUNNING)
		{
			i = -1
		}

		return state
	}

	//----------------------------------------------------------------------
	override fun cancel(entity: Entity, world: World)
	{
		i = -1

		for (i in 0 until children.size)
		{
			children.get(i).cancel(entity, world)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "Random"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}