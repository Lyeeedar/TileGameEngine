package com.lyeeedar.Components

inline fun Entity.isTransient() = this.transient() != null
class TransientComponent :NonDataComponent()
{
	override val type: ComponentType = ComponentType.Transient

	override fun reset()
	{

	}
}