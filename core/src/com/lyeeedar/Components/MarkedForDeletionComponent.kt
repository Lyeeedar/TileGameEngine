package com.lyeeedar.Components

fun Entity.markForDeletion(delay: Float, reason: String = "")
{
	val mfd = this.addOrGet(ComponentType.MarkedForDeletion) as MarkedForDeletionComponent
	mfd.set(delay, reason)
}

inline fun Entity.isMarkedForDeletion() = this.markedForDeletion() != null
class MarkedForDeletionComponent : AbstractComponent()
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