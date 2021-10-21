package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Systems.AbstractRenderSystem
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Renderable")
class ScreenJoltAction : AbstractOneShotActionSequenceAction()
{
	var amount: Float = 2f
	var joltDuration: Float = 0.1f

	override fun enter(state: ActionSequenceState)
	{
		val renderSystem = state.world.systems.filterIsInstance<AbstractRenderSystem>().first()
		renderSystem.renderer.setScreenJolt(amount, joltDuration)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		amount = xmlData.getFloat("Amount", 2f)
		joltDuration = xmlData.getFloat("JoltDuration", 0.1f)
	}
	override val classID: String = "ScreenJolt"
	//endregion
}