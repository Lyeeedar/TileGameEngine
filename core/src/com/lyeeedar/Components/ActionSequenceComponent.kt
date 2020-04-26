package com.lyeeedar.Components

import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.ActionSequence.ActionSequenceState

class ActionSequenceComponent : NonDataComponent()
{
	override val type: ComponentType = ComponentType.ActionSequence

	var actionSequence: ActionSequence? = null
	var actionSequenceState: ActionSequenceState? = null

	fun set(sequence: ActionSequence, state: ActionSequenceState): ActionSequenceComponent
	{
		this.actionSequence = sequence
		this.actionSequenceState = state

		return this
	}

	override fun reset()
	{
		actionSequence = null
		actionSequenceState = null
	}
}