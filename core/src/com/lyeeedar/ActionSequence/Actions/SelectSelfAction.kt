package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.position
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute", colour = "252,102,9", name = "PickSelf")
class SelectSelfAction : AbstractOneShotActionSequenceAction()
{
	init
	{
		permutesTargets = true
	}

	override fun enter(state: ActionSequenceState)
	{
		state.targets.clear()
		state.targets.add(state.source.get()!!.position()!!.position)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "SelectSelf"
	//endregion
}