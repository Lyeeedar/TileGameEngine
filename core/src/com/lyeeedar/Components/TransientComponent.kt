package com.lyeeedar.Components

inline fun Entity.isTransient() = this.transient() != null
class TransientComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Transient

	var blocksTurns = true

	override fun reset()
	{
		blocksTurns = true
	}
}