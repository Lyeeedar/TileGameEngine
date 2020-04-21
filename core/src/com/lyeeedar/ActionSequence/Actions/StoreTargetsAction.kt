package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import ktx.collections.toGdxArray

class StoreTargetsAction : AbstractActionSequenceAction()
{
	lateinit var key: String

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		state.data[key] = state.targets.toGdxArray()

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
		key = xmlData.get("Key")
	}
	override val classID: String = "StoreTargets"
	//endregion
}

class RestoreTargetsAction : AbstractActionSequenceAction()
{
	lateinit var key: String

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		state.targets.clear()

		val targets = state.data[key] as Array<Point>
		state.targets.addAll(targets)

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
		key = xmlData.get("Key")
	}
	override val classID: String = "RestoreTargets"
	//endregion
}