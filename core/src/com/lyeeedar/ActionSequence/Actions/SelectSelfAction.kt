package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.pos
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute", colour = "252,102,9")
class SelectSelfAction : AbstractOneShotActionSequenceAction()
{
	override fun enter(state: ActionSequenceState): ActionState
	{
		state.targets.clear()
		state.targets.add(state.source.pos()!!.position)

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "SelectSelf"
	//endregion
}