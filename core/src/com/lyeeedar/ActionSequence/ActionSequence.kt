package com.lyeeedar.ActionSequence

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.ActionSequence.Actions.AbstractActionSequenceAction
import com.lyeeedar.ActionSequence.Actions.ActionState
import com.lyeeedar.ActionSequence.Actions.RepeatBeginAction
import com.lyeeedar.ActionSequence.Actions.RepeatEndAction
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.pos
import com.lyeeedar.Direction
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import ktx.collections.set

class ActionSequenceState
{
	lateinit var source: Entity

	val enteredActions: Array<AbstractActionSequenceAction> = Array(false, 4)
	val targets: Array<Point> = Array(1)
	val lockedEntityTargets: Array<Entity> = Array(1)
	var facing: Direction = Direction.NORTH

	var blocked = false
	var currentTime: Float = 0f
	var index = 0

	var data = ObjectMap<String, Any>()

	init
	{
		targets.add(source.pos()!!.position)
	}
}

class ActionSequence : XmlDataClass()
{
	var cancellable = true
	var removeOnDeath = false
	val actions: Array<AbstractActionSequenceAction> = Array(4)

	fun onTurn(state: ActionSequenceState)
	{
		var anyBlocked = false
		for (action in state.enteredActions)
		{
			val actionState = action.onTurn(state)
			if (actionState == ActionState.Blocked)
			{
				anyBlocked = true
			}
		}
		state.blocked = anyBlocked
	}

	fun update(delta: Float, state: ActionSequenceState): Boolean
	{
		if (state.blocked)
		{
			return false
		}

		if (state.lockedEntityTargets.size > 0)
		{
			state.targets.clear()
			for (target in state.lockedEntityTargets)
			{
				val pos = target.pos() ?: continue
				state.targets.add(pos.position)
			}
		}

		state.currentTime += delta

		val itr = state.enteredActions.iterator()
		while (itr.hasNext())
		{
			val action = itr.next()
			if (action.end <= state.currentTime)
			{
				val actionState = action.exit(state)
				if (actionState != ActionState.Blocked)
				{
					itr.remove()
				}
			}
		}

		if (state.index >= actions.size)
		{
			if (state.enteredActions.size == 0)
			{
				state.blocked = false
				return true
			}
		}

		for (i in state.index until actions.size)
		{
			val action = actions[i]
			if (action.start <= state.currentTime)
			{
				state.index = i + 1

				val actionState = action.enter(state)
				if (actionState == ActionState.Blocked)
				{
					state.blocked = true
					state.currentTime = action.start
					state.enteredActions.add(action)
					break
				}
				else
				{
					if (action.end <= state.currentTime)
					{
						action.exit(state)
					}
					else
					{
						state.enteredActions.add(action)
					}
				}
			}
			else
			{
				state.index = i
				break
			}
		}

		return false
	}

	fun cancel(state: ActionSequenceState)
	{
		for (action in state.enteredActions)
		{
			action.exit(state)
		}
	}

	var obtained: Boolean = false
	companion object
	{
		val loadedSequences = IntMap<ActionSequence>()
		fun load(xmlData: XmlData): ActionSequence
		{
			val hash = xmlData.hashCode()
			val existing = loadedSequences[hash]
			if (existing != null) return existing

			val actions = Array<AbstractActionSequenceAction>(4)
			val sequence = ActionSequence()
			sequence.load(xmlData)

			for (timelineEl in xmlData.children)
			{
				for (actionEl in timelineEl.children)
				{
					val action = AbstractActionSequenceAction.load(actionEl, sequence)
					actions.add(action)
				}
			}

			val sorted = actions.sortedBy { it.start }

			var i = 0
			while (i < sorted.size)
			{
				val currentAction = sorted[i]

				if (currentAction is RepeatBeginAction)
				{
					// search forward to find the end, store items in list
					val actionsToRepeat = Array<AbstractActionSequenceAction>()
					var endI = i+1

					while (endI < sorted.size)
					{
						val endAction = sorted[endI]

						if (endAction is RepeatBeginAction)
						{
							throw Exception("Cannot nest Repeat blocks!")
						}
						else if (endAction is RepeatEndAction)
						{
							break
						}
						else
						{
							actionsToRepeat.add(endAction)
						}

						endI++
					}

					if (endI == sorted.size)
					{
						throw Exception("Repeat block must have a RepeatEnd!")
					}

					val start = currentAction.start
					val end = sorted[endI].end

					// for num count, copy items and place into array
					var currentTime = start
					for (repeatNum in 0 until currentAction.count)
					{
						for (action in actionsToRepeat)
						{
							val offset = action.start - start
							val newStart = currentTime + offset
							val newDuration = action.end - action.start

							val newAction = action.copy(sequence)
							newAction.start = newStart
							newAction.duration = newDuration

							sequence.actions.add(newAction)
						}

						currentTime += end - start
					}

					i = endI+1

					val totalOffset = currentTime - end

					// move all remaining actions along
					for (i in 0 until sorted.size)
					{
						val action = sorted[i]

						if (action.start >= end)
						{
							action.start += totalOffset
						}

						if (action.end >= end)
						{
							action.end += totalOffset
						}
					}
				}
				else
				{
					sequence.actions.add(currentAction)
				}

				i++
			}

			loadedSequences[hash] = sequence

			return sequence
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val actionsEl = xmlData.getChildByName("Actions")
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val obj = XmlDataClassLoader.loadAbstractActionSequenceAction(el.get("classID"))
				obj.load(el)
				actions.add(obj)
			}
		}
		obtained = xmlData.getBoolean("Obtained", false)
	}
	//endregion
}