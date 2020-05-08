package com.lyeeedar.ActionSequence

import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.Actions.AbstractActionSequenceAction
import com.lyeeedar.ActionSequence.Actions.ActionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Components.position
import com.lyeeedar.Direction
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader
import java.util.*
import ktx.collections.set
import squidpony.squidmath.LightRNG

class ActionSequenceState
{
	lateinit var source: EntityReference
	lateinit var sourcePoint: Point
	lateinit var world: World<*>

	val enteredActions: ObjectSet<AbstractActionSequenceAction> = ObjectSet()
	val targets: Array<Point> = Array(1)
	val lockedEntityTargets: Array<EntityReference> = Array(1)
	var facing: Direction = Direction.NORTH

	var rng = LightRNG()

	var blocked = false
	var currentTime: Float = 0f
	var index = 0

	var data = ObjectMap<String, Any?>()
	var uid = 0

	fun set(source: EntityReference, world: World<*>, seed: Long): ActionSequenceState
	{
		uid = Random.sharedRandom.nextInt()

		this.source = source
		this.world = world

		sourcePoint = source.get()?.position()?.position ?: Point.ONE

		targets.clear()
		targets.add(source.entity.position()!!.position)

		rng.setSeed(seed)

		return this
	}

	fun reset()
	{
		targets.clear()
		enteredActions.clear()
		lockedEntityTargets.clear()
		facing = Direction.NORTH

		blocked = false
		currentTime = 0f
		index = 0

		data.clear()

		uid = Random.sharedRandom.nextInt()
	}

	fun writeVariables(map: ObjectFloatMap<String>)
	{
		for (entry in data)
		{
			val value = entry.value
			map[entry.key.toLowerCase(Locale.ENGLISH)] = when (value)
			{
				is Float -> value
				is Int -> value.toFloat()
				else -> 1f
			}
		}
	}
}

@DataFile(colour="228,78,255", icon="Sprites/EffectSprites/Explosion/Explosion_2.png")
@DataClass(implementsStaticLoad = true)
class ActionSequence(val xml: XmlData) : XmlDataClass()
{
	var cancellable = true

	@DataValue(dataName = "Actions")
	@DataTimeline(timelineGroup = true)
	val rawActions: Array<AbstractActionSequenceAction> = Array(4)

	//region non-data
	val triggers = Array<AbstractActionSequenceTrigger>(4)
	//endregion

	override fun afterLoad()
	{
		for (action in rawActions)
		{
			triggers.add(EnterTrigger(action))
			triggers.add(ExitTrigger(action))
		}
		triggers.sort(compareBy { it.time })
	}

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

		if (!state.source.isValid())
		{
			cancel(state)
			return true
		}

		if (state.lockedEntityTargets.size > 0)
		{
			state.targets.clear()
			val itr = state.lockedEntityTargets.iterator()
			while (itr.hasNext())
			{
				val entity = itr.next()
				if (entity.isValid())
				{
					val pos = entity.entity.position() ?: continue
					state.targets.add(pos.position)
				}
				else
				{
					itr.remove()
				}
			}
		}

		state.currentTime += delta

		while(state.index < triggers.size)
		{
			val trigger = triggers[state.index]
			if (trigger.time <= state.currentTime)
			{
				val actionState = trigger.executeTrigger(state)
				if (actionState == ActionState.Blocked)
				{
					state.blocked = true
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

		if (state.index >= triggers.size)
		{
			return true
		}

		return false
	}

	fun cancel(state: ActionSequenceState)
	{
		for (action in state.enteredActions)
		{
			action.exit(state)
		}
		state.enteredActions.clear()
	}

	fun loadDuplicate(): ActionSequence
	{
		val sequence = ActionSequence(xml)
		sequence.load(xml)

		return sequence
	}

	companion object
	{
		val loadedSequences = IntMap<ActionSequence>()
		fun load(path: String): ActionSequence
		{
			val xml = getXml(path)
			return load(xml)
		}

		fun load(xmlData: XmlData): ActionSequence
		{
			val hash = xmlData.hashCode()
			val existing = loadedSequences[hash]
			if (existing != null) return existing

			val sequence = ActionSequence(xmlData)
			sequence.load(xmlData)

			loadedSequences[hash] = sequence

			return sequence
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val rawActionsEl = xmlData.getChildByName("Actions")
		if (rawActionsEl != null)
		{
			for (el in rawActionsEl.children)
			{
				for (keyframeEl in el.children)
				{
					val objrawActions = XmlDataClassLoader.loadAbstractActionSequenceAction(keyframeEl.get("classID", keyframeEl.name)!!)
					objrawActions.load(keyframeEl)
					rawActions.add(objrawActions)
				}
			}
		}
		rawActions.sort(compareBy{ it.time })
		afterLoad()
	}
	//endregion
}

abstract class AbstractActionSequenceTrigger(val action: AbstractActionSequenceAction, val time: Float)
{
	abstract fun executeTrigger(state: ActionSequenceState): ActionState
}
class EnterTrigger(action: AbstractActionSequenceAction) : AbstractActionSequenceTrigger(action, action.time)
{
	override fun executeTrigger(state: ActionSequenceState): ActionState
	{
		state.enteredActions.add(action)
		action.enter(state)
		return ActionState.Completed
	}
}

class ExitTrigger(action: AbstractActionSequenceAction) : AbstractActionSequenceTrigger(action, action.end)
{
	override fun executeTrigger(state: ActionSequenceState): ActionState
	{
		val actionState = action.exit(state)
		if (actionState != ActionState.Blocked)
		{
			state.enteredActions.remove(action)
		}

		return actionState
	}
}