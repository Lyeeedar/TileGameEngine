package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Meta", colour = "199,18,117", name = "Block")
class BlockTurnAction : AbstractOneShotActionSequenceAction()
{
	val key = "blocked" + this.hashCode()
	var turns: Int = 1

	override fun onTurn(state: ActionSequenceState)
	{
		var counter = state.data[key] as Int
		counter--
		state.data[key] = counter
	}

	override fun enter(state: ActionSequenceState)
	{
		state.data[key] = turns
	}

	override fun exit(state: ActionSequenceState)
	{
		val counter = state.data[key] as? Int ?: 0

		if (counter <= 0)
		{
			state.data.remove(key)
		}
	}

	override fun isBlocked(state: ActionSequenceState): Boolean
	{
		val counter = state.data[key] as? Int ?: 0
		return counter > 0
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