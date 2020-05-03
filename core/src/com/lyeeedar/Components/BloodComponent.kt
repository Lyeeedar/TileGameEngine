package com.lyeeedar.Components

class BloodComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Blood

	var totalTurns = 100
	var remainingTurns = totalTurns

	var originalA = 1f
	var originalScale = 1f

	override fun reset()
	{
		remainingTurns = totalTurns
	}
}