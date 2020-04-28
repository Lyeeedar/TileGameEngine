package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import ktx.collections.toGdxArray

@DataClass(category = "Permute", colour = "252,102,9")
class StoreTargetsAction : AbstractOneShotActionSequenceAction()
{
	lateinit var key: String

	override fun enter(state: ActionSequenceState)
	{
		state.data[key] = state.targets.toGdxArray()
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

@DataClass(category = "Permute", colour = "252,102,9")
class RestoreTargetsAction : AbstractOneShotActionSequenceAction()
{
	lateinit var key: String

	override fun enter(state: ActionSequenceState)
	{
		state.targets.clear()

		val targets = state.data[key] as Array<Point>
		state.targets.addAll(targets)
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