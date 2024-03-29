package com.lyeeedar.ActionSequence

import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.Actions.AbstractActionSequenceAction
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Components.position
import com.lyeeedar.Direction
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import java.util.*
import ktx.collections.set
import squidpony.squidmath.LightRNG

class ActionSequenceReference private constructor(val state: ActionSequenceState)
{
	val id = state.usageID

	fun isValid(): Boolean
	{
		if (state.usageID != id) return false

		return true
	}

	fun get(): ActionSequenceState?
	{
		if (isValid()) return state

		return null
	}

	override fun hashCode(): Int
	{
		return state.hashCode() + id
	}

	override fun equals(other: Any?): Boolean
	{
		if (other is ActionSequenceState)
		{
			return other == state && other.usageID == id
		}
		else if (other is ActionSequenceReference)
		{
			return other.hashCode() == hashCode()
		}
		else
		{
			return false
		}
	}

	companion object
	{
		fun create(state: ActionSequenceState): ActionSequenceReference
		{
			return ActionSequenceReference(state)
		}
	}
}

class ActionSequenceState
{
	var usageID = 0

	lateinit var source: EntityReference
	lateinit var sourcePoint: Point
	lateinit var world: World<*>
	lateinit var sequence: ActionSequence

	val enteredActions: Array<AbstractActionSequenceAction> = Array(false, 2)
	val targets: Array<Point> = Array(false, 1)
	val lockedEntityTargets: Array<EntityReference> = Array(false, 1)
	var facing: Direction = Direction.NORTH

	var rng = LightRNG()

	var currentTime: Float = 0f
	var index = 0
	var completed = false

	var cachedDelayedState = false

	var delay = 0f

	var detached = false

	var data = ObjectMap<String, Any?>()
	var uid = 0

	fun set(source: EntityReference, sequence: ActionSequence, world: World<*>, seed: Long): ActionSequenceState
	{
		uid = Random.sharedRandom.nextInt()

		this.source = source
		this.world = world
		this.sequence = sequence

		this.cachedDelayedState = false

		sourcePoint = source.get()?.position()?.position ?: Point.ONE

		targets.clear()
		targets.add(source.get()!!.position()!!.position)

		rng.setSeed(seed)

		delay = 0f

		usageID++

		return this
	}

	fun reset()
	{
		targets.clear()
		enteredActions.clear()
		lockedEntityTargets.clear()
		facing = Direction.NORTH

		completed = false
		currentTime = 0f
		index = 0
		delay = 0f
		cachedDelayedState = false

		data.clear()

		uid = Random.sharedRandom.nextInt()

		usageID++
	}

	fun writeVariables(map: ObjectFloatMap<String>)
	{
		for (entry in data)
		{
			val value = entry.value
			map[entry.key.lowercase(Locale.ENGLISH)] = when (value)
			{
				is Float -> value
				is Int -> value.toFloat()
				else -> 1f
			}
		}
	}

	private var currentRef: ActionSequenceReference? = null
	fun getRef(): ActionSequenceReference
	{
		if (currentRef != null && currentRef!!.isValid()) return currentRef!!

		currentRef = ActionSequenceReference.create(this)

		return currentRef!!
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
		for (i in 0 until rawActions.size)
		{
			val action = rawActions[i]

			triggers.add(EnterTrigger(action))
			triggers.add(ExitTrigger(action))
		}
		triggers.sort(compareBy { it.time })

		for (i in 0 until rawActions.size)
		{
			val action = rawActions[i]
			action.afterLoad(this)
		}
	}

	fun preTurn(state: ActionSequenceState)
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			action.preTurn(state)
		}
	}

	fun onTurn(state: ActionSequenceState)
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			action.onTurn(state)
		}
	}

	fun removeFromTiles(state: ActionSequenceState)
	{
		for (i in 0 until state.targets.size)
		{
			val point = state.targets[i]
			val tile = state.world.grid.tryGet(point, null) ?: continue
			tile.runningSequences.remove(state.getRef())
		}
	}

	fun addToTiles(state: ActionSequenceState)
	{
		for (i in 0 until state.targets.size)
		{
			val point = state.targets[i]
			val tile = state.world.grid.tryGet(point, null) ?: continue
			tile.runningSequences.add(state.getRef())
		}
	}

	fun isBlocked(state: ActionSequenceState): Boolean
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			if (action.isBlocked(state))
			{
				return true
			}
		}

		return false
	}

	fun isDelayed(state: ActionSequenceState): Boolean
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			if (action.isDelayed(state))
			{
				return true
			}
		}

		return false
	}

	fun update(delta: Float, state: ActionSequenceState): Boolean
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			action.update(delta, state)
		}

		if (isBlocked(state))
		{
			return false
		}

		if (!state.source.isValid())
		{
			cancel(state)
			return true
		}

		if (state.delay > 0f)
		{
			state.delay -= delta
			return false
		}

		removeFromTiles(state)

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

		if (state.index >= triggers.size)
		{
			state.completed = true
			return true
		}

		addToTiles(state)

		return false
	}

	fun cancel(state: ActionSequenceState)
	{
		for (i in 0 until state.enteredActions.size)
		{
			val action = state.enteredActions[i]
			action.cancel(state)
			action.exit(state)
		}
		state.enteredActions.clear()
		state.completed = true
		removeFromTiles(state)
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

abstract class AbstractActionSequenceTrigger(val action: AbstractActionSequenceAction, var time: Float)
{
	abstract fun executeTrigger(state: ActionSequenceState): Boolean
}
class EnterTrigger(action: AbstractActionSequenceAction) : AbstractActionSequenceTrigger(action, action.time)
{
	override fun executeTrigger(state: ActionSequenceState): Boolean
	{
		state.enteredActions.add(action)
		action.enter(state)
		return false
	}
}

class ExitTrigger(action: AbstractActionSequenceAction) : AbstractActionSequenceTrigger(action, action.end)
{
	override fun executeTrigger(state: ActionSequenceState): Boolean
	{
		if (action.isDelayed(state)) return true

		action.exit(state)
		val blocked = action.isBlocked(state)
		if (!blocked)
		{
			state.enteredActions.removeValue(action, true)
		}

		return blocked
	}
}