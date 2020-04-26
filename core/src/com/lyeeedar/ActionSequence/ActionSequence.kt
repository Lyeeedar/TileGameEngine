package com.lyeeedar.ActionSequence

import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.Actions.AbstractActionSequenceAction
import com.lyeeedar.ActionSequence.Actions.ActionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.position
import com.lyeeedar.Direction
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set

class ActionSequenceState
{
	lateinit var source: Entity
	lateinit var world: World<*>

	val enteredActions: Array<AbstractActionSequenceAction> = Array(false, 4)
	val targets: Array<Point> = Array(1)
	val lockedEntityTargets: Array<Entity> = Array(1)
	var facing: Direction = Direction.NORTH

	var seed: Long = 0

	var blocked = false
	var currentTime: Float = 0f
	var index = 0

	var data = ObjectMap<String, Any?>()

	fun set(source: Entity, world: World<*>): ActionSequenceState
	{
		this.source = source
		this.world = world

		targets.clear()
		targets.add(source.position()!!.position)

		return this
	}

	fun reset()
	{
		targets.clear()
		enteredActions.clear()
		lockedEntityTargets.clear()
		facing = Direction.NORTH

		seed = 0
		blocked = false
		currentTime = 0f
		index = 0

		data.clear()
	}

	fun writeVariables(map: ObjectFloatMap<String>)
	{
		for (entry in data)
		{
			val value = entry.value
			map[entry.key] = when (value)
			{
				is Float -> value
				is Int -> value.toFloat()
				else -> 1f
			}
		}
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<ActionSequenceState> = object : Pool<ActionSequenceState>() {
			override fun newObject(): ActionSequenceState
			{
				return ActionSequenceState()
			}
		}

		fun obtain(): ActionSequenceState
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.obtained = true

			obj.reset()

			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}

@DataFile(colour="228,78,255", icon="Sprites/EffectSprites/Explosion/Explosion_2.png")
class ActionSequence : XmlDataClass()
{
	var cancellable = true
	var removeOnDeath = false

	@DataTimeline(timelineGroup = true)
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
				val pos = target.position() ?: continue
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
			if (action.time <= state.currentTime)
			{
				state.index = i + 1

				val actionState = action.enter(state)
				if (actionState == ActionState.Blocked)
				{
					state.blocked = true
					state.currentTime = action.time
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

			val sequence = ActionSequence()
			sequence.load(xmlData)

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
				for (keyframeEl in el.children)
				{
					val objactions = XmlDataClassLoader.loadAbstractActionSequenceAction(keyframeEl.get("classID", keyframeEl.name)!!)
					objactions.load(keyframeEl)
					actions.add(objactions)
				}
			}
		}
		actions.sort(compareBy{ it.time })
		afterLoad()
	}
	//endregion
}