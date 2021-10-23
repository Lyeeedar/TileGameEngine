package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.ActionSequence.AbstractActionSequenceTrigger
import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.set

val targetDelayKey = "TargetDelay"

@DataClass(category = "Meta", name = "ForEachTarget")
class ProcessTargetsIndividuallyAction : AbstractDurationActionSequenceAction()
{
	val dataKey = "ProcessTargets" + this.hashCode()

	val containedTriggers = Array<AbstractActionSequenceTrigger>()

	override fun afterLoad(actionSequence: ActionSequence)
	{
		// remove all the contained triggers from the sequence
		val itr = actionSequence.triggers.iterator()

		var inside = false
		while (itr.hasNext())
		{
			val trigger = itr.next()
			if (trigger.action == this)
			{
				if (!inside)
				{
					inside = true
				}
				else
				{
					break
				}
			}
			else if (trigger.action.time >= time)
			{
				itr.remove()
				containedTriggers.add(trigger)
				trigger.time -= time
			}
		}
	}

	override fun update(delta: Float, state: ActionSequenceState)
	{
		val substates = state.data[dataKey] as? Array<ActionSequenceState> ?: return
		for (state in substates)
		{
			state.currentTime += delta

			while(state.index < containedTriggers.size)
			{
				val trigger = containedTriggers[state.index]
				if (trigger.time <= state.currentTime)
				{
					val blocked = trigger.executeTrigger(state)
					if (blocked)
					{
						state.currentTime = trigger.time
						break
					}
				}
				else
				{
					break
				}

				state.index++
			}

			if (state.index >= containedTriggers.size)
			{
				state.completed = true
				substates.removeValue(state, true)
			}
		}
	}

	override fun enter(state: ActionSequenceState)
	{
		val targetDelay = state.data[targetDelayKey] as? ObjectFloatMap<Point> ?: ObjectFloatMap()

		val substates = Array<ActionSequenceState>()
		for (target in state.targets)
		{
			val subState = ActionSequenceState()
			subState.set(state.source, state.sequence, state.world, state.rng.nextLong())
			subState.targets.add(target)
			subState.facing = state.facing

			subState.currentTime = -targetDelay[target, 0f]

			substates.add(subState)
		}
		state.data[dataKey] = substates
	}

	override fun exit(state: ActionSequenceState)
	{
		val substates = state.data[dataKey] as? Array<ActionSequenceState> ?: return
		if (substates.size != 0)
		{
			// force complete
			update(Float.MAX_VALUE, state)
		}
		state.data.remove(dataKey)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "ProcessTargetsIndividually"
	//endregion
}