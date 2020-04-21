package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Meta", colour = "255,0,0,255")
class RepeatBeginAction : AbstractActionSequenceAction()
{
	var count: Int = 1

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		count = xmlData.getInt("Count", 1)
	}
	override val classID: String = "RepeatBegin"
	//endregion
}

@DataClass(category = "Meta", colour = "255,0,0,255")
class RepeatEndAction : AbstractActionSequenceAction()
{
	override fun onTurn(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		throw UnsupportedOperationException()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "RepeatEnd"
	//endregion
}