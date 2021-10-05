package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Systems.AbstractRenderSystem
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Renderable")
class ScreenShakeAction : AbstractDurationActionSequenceAction()
{
	var speed: Float = 10f
	var amount: Float = 5f

	override fun enter(state: ActionSequenceState)
	{
		val renderSystem = state.world.systems.filterIsInstance<AbstractRenderSystem>().first()
		renderSystem.renderer.setScreenShake(amount, 1f / speed)
		renderSystem.renderer.lockScreenShake()
	}

	override fun exit(state: ActionSequenceState)
	{
		val renderSystem = state.world.systems.filterIsInstance<AbstractRenderSystem>().first()
		renderSystem.renderer.unlockScreenShake()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		speed = xmlData.getFloat("Speed", 10f)
		amount = xmlData.getFloat("Amount", 5f)
	}
	override val classID: String = "ScreenShake"
	//endregion
}