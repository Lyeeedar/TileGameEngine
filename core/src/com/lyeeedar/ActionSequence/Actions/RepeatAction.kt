package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Meta", colour = "255,0,0,255")
class RepeatAction : AbstractDurationActionSequenceAction()
{
	val key = "repeat"
	var count: Int = 1

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		val count = state.data[key] as Int
		return if (count <= 0) ActionState.Completed else ActionState.Blocked
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		if (state.data.containsKey(key))
		{
			var count = state.data[key] as Int
			count--
			state.data[key] = count
		}
		else
		{
			state.data[key] = count
			state.data["i"] = state.index
		}

		return ActionState.Blocked
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val count = state.data[key] as Int

		if (count <= 0)
		{
			state.data.remove(key)
			return ActionState.Completed
		}
		else
		{
			state.currentTime = time
			state.index = state.data["i"] as Int

			val itr = state.enteredActions.iterator()
			while (itr.hasNext())
			{
				val action = itr.next()
				if (action.time < time)
				{
					action.exit(state)
					itr.remove()
				}
			}

			return ActionState.Blocked
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		count = xmlData.getInt("Count", 1)
		afterLoad()
	}
	override val classID: String = "Repeat"
	//endregion
}