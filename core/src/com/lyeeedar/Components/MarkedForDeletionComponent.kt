package com.lyeeedar.Components

fun Entity.markForDeletion(delay: Float, reason: String = "")
{
	this.addComponent(ComponentType.MarkedForDeletion)
	this.markedForDeletion()!!.set(delay, reason)
}

inline fun Entity.isMarkedForDeletion() = this.markedForDeletion() != null
class MarkedForDeletionComponent : NonDataComponent()
{
	override val type: ComponentType = ComponentType.MarkedForDeletion

	var deletionEffectDelay: Float = 0f
	var reason: String = ""

	fun set(delay: Float, reason: String = ""): MarkedForDeletionComponent
	{
		deletionEffectDelay = delay
		this.reason = reason
		return this
	}

	override fun reset()
	{
		deletionEffectDelay = 0f
	}
}