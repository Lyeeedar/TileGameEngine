package com.lyeeedar.Components

inline fun Entity.isTransient() = this.transient() != null
inline fun Entity.transient(): TransientComponent? = this.components[ComponentType.Transient] as TransientComponent?
class TransientComponent : AbstractComponent<EmptyComponentData>(EmptyComponentData())
{
	override val type: ComponentType = ComponentType.Transient

	override fun reset()
	{

	}
}