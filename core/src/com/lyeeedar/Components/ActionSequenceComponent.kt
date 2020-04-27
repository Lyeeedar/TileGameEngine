package com.lyeeedar.Components

import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.ActionSequence.ActionSequenceState

class ActionSequenceComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.ActionSequence

	lateinit var actionSequence: ActionSequence
	val actionSequenceState: ActionSequenceState = ActionSequenceState()

	fun set(sequence: ActionSequence): ActionSequenceComponent
	{
		this.actionSequence = sequence
		return this
	}

	override fun reset()
	{
		actionSequenceState.reset()
	}
}