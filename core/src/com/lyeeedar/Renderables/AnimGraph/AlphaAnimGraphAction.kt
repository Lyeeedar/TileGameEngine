package com.lyeeedar.Renderables.AnimGraph

import com.lyeeedar.Renderables.AnimationGraphState
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData

class AlphaAnimGraphAction : AbstractAnimGraphAction()
{
	lateinit var alphaRule: CompiledExpression

	override fun enter(state: AnimationGraphState)
	{

	}

	override fun update(delta: Float, state: AnimationGraphState)
	{
		val slot = state.getSlot(slot)
		slot.color.a = alphaRule.evaluate(state.variables, Random.sharedRandom)
	}

	override fun exit(state: AnimationGraphState)
	{

	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		alphaRule = CompiledExpression(xmlData.get("AlphaRule", "1")!!)
	}
	override val classID: String = "Alpha"
	//endregion
}