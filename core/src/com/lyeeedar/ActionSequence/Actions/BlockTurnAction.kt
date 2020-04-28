package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Meta", colour = "199,18,117")
class BlockTurnAction : AbstractOneShotActionSequenceAction()
{
	val key = "blocked"
	var turns: Int = 1

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		var counter = state.data[key] as Int
		counter--
		state.data[key] = counter

		return if (counter <= 0) ActionState.Completed else ActionState.Blocked
	}

	override fun enter(state: ActionSequenceState)
	{
		state.data[key] = turns
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val counter = state.data[key] as Int

		if (counter <= 0)
		{
			state.data.remove(key)
			return ActionState.Completed
		}
		else
		{
			return ActionState.Blocked
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		turns = xmlData.getInt("Turns", 1)
	}
	override val classID: String = "BlockTurn"
	//endregion
}