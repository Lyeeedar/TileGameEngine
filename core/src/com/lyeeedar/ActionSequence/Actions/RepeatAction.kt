package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Meta", colour = "255,0,0,255")
class RepeatAction : AbstractDurationActionSequenceAction()
{
	val key = "repeat" + this.hashCode()
	var count: Int = 1

	override fun enter(state: ActionSequenceState)
	{
		if (state.data.containsKey(key))
		{
			var count = state.data[key] as Int
			count--
			state.data[key] = count
		}
		else
		{
			state.data[key] = count
			state.data["i"] = state.index-1
		}
	}

	override fun exit(state: ActionSequenceState)
	{
		val count = state.data[key] as Int

		if (count <= 0)
		{
			state.data.remove(key)
		}
		else
		{
			state.currentTime = time
			state.index = state.data["i"] as Int
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		count = xmlData.getInt("Count", 1)
	}
	override val classID: String = "Repeat"
	//endregion
}