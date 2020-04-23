package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.*
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.set

enum class EventType
{
	// Actions
	ATTACK,
	MOVE,
	USE_ABILITY,

	// Self events
	DEAL_DAMAGE,
	TAKE_DAMAGE,
	HEALED,
	CRIT,
	BLOCK,
	KILL,

	// General events
	DEATH,
	ON_TURN;
}

class EventSystem(world: World) : AbstractEntitySystem(world, EntitySignature().all(ComponentType.EventHandler))
{
	private val queuedEvents = Array<EventData>()
	private val executingArray = Array<EventData>()

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{

	}

	override fun onTurnEntity(entity: Entity)
	{
		if (isEventRegistered(EventType.ON_TURN, entity))
		{
			addEvent(EventData.obtain().set(EventType.ON_TURN, entity, entity))
		}
	}

	override fun beforeOnTurn()
	{
		executingArray.clear()
		executingArray.addAll(queuedEvents)
		queuedEvents.clear()

		for (event in executingArray)
		{
			if (event.source.isMarkedForDeletion())
			{
				event.free()
				continue
			}

			val stats = event.source.stats()
			if (stats != null)
			{

			}

			val eventHandler = event.source.eventHandler()
			if (eventHandler != null)
			{
				checkHandlers(event, eventHandler.data.handlers)
			}

			event.free()
		}
	}

	private fun checkHandlers(eventData: EventData, eventHandler: FastEnumMap<EventType, Array<EventAndCondition>>)
	{
		val handlers = eventHandler[eventData.type]

		if (handlers != null)
		{
			for (handler in handlers)
			{
				if (handler.condition.evaluate(eventData.variables) != 0f)
				{
					val sequence = handler.sequence
					val state = ActionSequenceState()
					state.set(eventData.source, world)
					state.targets.clear()
					state.targets.addAll(eventData.targets)

					if (eventData.targetEntity != null)
					{
						state.lockedEntityTargets.add(eventData.targetEntity!!)
					}

					val entity = spawnedEventEntityArchetype.build()

					val comp = entity.actionSequence()!!
					comp.actionSequence = sequence
					comp.actionSequenceState = state

					world.addEntity(entity)
				}
			}
		}
	}

	fun addEvent(eventData: EventData)
	{
		queuedEvents.add(eventData)
	}

	companion object
	{
		val spawnedEventEntityArchetype = EntityArchetypeBuilder()
			.add(ComponentType.ActionSequence)
			.add(ComponentType.Transient)

		fun isEventRegistered(type: EventType, entity: Entity): Boolean
		{
			val stats = entity.stats()
			if (stats != null)
			{

			}

			val eventHandler = entity.eventHandler()
			if (eventHandler != null)
			{
				if ((eventHandler.data.handlers[type]?.size ?: 0) > 0)
				{
					return true
				}
			}

			return false
		}
	}
}

class EventData()
{
	lateinit var type: EventType
	lateinit var source: Entity
	val targets = Array<Point>(1)
	val variables = ObjectFloatMap<String>()

	var targetEntity: Entity? = null

	fun set(type: EventType, source: Entity, target: Entity, inputVariables: Map<String, Float>? = null): EventData
	{
		this.type = type
		this.source = source
		this.targets.add(target.pos()!!.position)
		targetEntity = target

		if (inputVariables != null)
		{
			for (pair in inputVariables)
			{
				variables[pair.key] = pair.value
			}
		}

		source.stats()?.write(variables, "self")
		target.stats()?.write(variables, "target")

		return this
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<EventData> = object : Pool<EventData>() {
			override fun newObject(): EventData
			{
				return EventData()
			}
		}

		@JvmStatic fun obtain(): EventData
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}