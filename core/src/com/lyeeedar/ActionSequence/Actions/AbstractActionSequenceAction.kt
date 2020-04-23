package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader

enum class ActionState
{
	Completed,
	Blocked
}

abstract class AbstractActionSequenceAction : XmlDataClass()
{
	var time: Float = 0f

	//region non-data
	abstract val end: Float
	//endregion

	abstract fun onTurn(state: ActionSequenceState): ActionState
	abstract fun enter(state: ActionSequenceState): ActionState
	abstract fun exit(state: ActionSequenceState): ActionState

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

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "AbstractOneShot"
	//endregion
}

abstract class AbstractDurationActionSequenceAction : AbstractActionSequenceAction()
{
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
	override val classID: String = "AbstractDuration"
	//endregion
}