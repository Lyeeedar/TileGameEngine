package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class BlockTurnAction : AbstractActionSequenceAction()
{
	var turns: Int = 1

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		var counter = state.data["blocked"] as Int
		counter--
		state.data["blocked"] = counter

		return if (counter <= 0) ActionState.Completed else ActionState.Blocked
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		state.data["blocked"] = turns

		return ActionState.Blocked
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val counter = state.data["blocked"] as Int
		return if (counter <= 0) ActionState.Completed else ActionState.Blocked
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