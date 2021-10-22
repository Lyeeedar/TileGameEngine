package com.lyeeedar.Renderables.AnimGraph

import com.lyeeedar.Renderables.AnimationGraphState
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader

abstract class AbstractAnimGraphAction : XmlDataClass()
{
	@DataCompiledExpression(default = "1")
	lateinit var condition: CompiledExpression

	lateinit var slot: String

	abstract fun enter(state: AnimationGraphState)
	abstract fun update(delta: Float, state: AnimationGraphState)
	abstract fun exit(state: AnimationGraphState)

	//region generated
	override fun load(xmlData: XmlData)
	{
		condition = CompiledExpression(xmlData.get("Condition", "1")!!)
		slot = xmlData.get("Slot")
	}
	abstract val classID: String
	//endregion
}