package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute", colour = "247,176,78", name = "SetSource")
class SetSourcePointAction : AbstractOneShotActionSequenceAction()
{
	init
	{
		permutesTargets = true
	}

	override fun enter(state: ActionSequenceState)
	{
		if (state.targets.size > 0)
		{
			state.sourcePoint = state.targets[0]
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "SetSourcePoint"
	//endregion
}