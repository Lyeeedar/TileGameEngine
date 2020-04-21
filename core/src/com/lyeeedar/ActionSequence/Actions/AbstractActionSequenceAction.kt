package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

enum class ActionState
{
	Completed,
	Blocked
}

abstract class AbstractActionSequenceAction : XmlDataClass(), Cloneable
{
	var time: Float = 0f
	var duration: Float = 0f

	val end: Float
		get() = time + duration

	abstract fun onTurn(state: ActionSequenceState): ActionState
	abstract fun enter(state: ActionSequenceState): ActionState
	abstract fun exit(state: ActionSequenceState): ActionState

	//region generated
	override fun load(xmlData: XmlData)
	{
		time = xmlData.getFloat("Time", 0f)
		duration = xmlData.getFloat("Duration", 0f)
	}
	abstract val classID: String
	//endregion
}