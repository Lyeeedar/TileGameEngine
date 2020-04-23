package com.lyeeedar.Components

import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.ActionSequence.ActionSequenceState

inline fun Entity.actionSequence(): ActionSequenceComponent? = this.components[ComponentType.ActionSequence] as ActionSequenceComponent?
class ActionSequenceComponent : NonDataComponent()
{
	override val type: ComponentType = ComponentType.ActionSequence

	var actionSequence: ActionSequence? = null
	var actionSequenceState: ActionSequenceState? = null

	override fun reset()
	{
		actionSequence = null
		actionSequenceState = null
	}
}