package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataNumericRange
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

abstract class AbstractActionSequenceAction : XmlDataClass()
{
	@DataNumericRange(min = 0f)
	var time: Float = 0f

	//region non-data
	abstract val end: Float
	//endregion

	open fun preTurn(state: ActionSequenceState) {}
	open fun onTurn(state: ActionSequenceState) {}
	abstract fun enter(state: ActionSequenceState)
	abstract fun exit(state: ActionSequenceState)
	open fun isBlocked(state: ActionSequenceState): Boolean = false

	//region generated
	override fun load(xmlData: XmlData)
	{
		time = xmlData.getFloat("Time", 0f)
	}
	abstract val classID: String
	//endregion
}

abstract class AbstractOneShotActionSequenceAction : AbstractActionSequenceAction()
{
	//region non-data
	override val end: Float
		get() = time
	//endregion

	override fun exit(state: ActionSequenceState)
	{

	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	//endregion
}

abstract class AbstractDurationActionSequenceAction : AbstractActionSequenceAction()
{
	@DataNumericRange(min = 0f)
	var duration: Float = 0f

	//region non-data
	override val end: Float
		get() = time + duration
	//endregion

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		duration = xmlData.getFloat("Duration", 0f)
	}
	//endregion
}