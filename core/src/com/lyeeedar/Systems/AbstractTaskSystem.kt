package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.lyeeedar.AI.Tasks.AbstractTask
import com.lyeeedar.Components.*

abstract class AbstractTaskSystem(world: World<*>) : AbstractSystem(world)
{
	private val taskEntities = world.getEntitiesFor().all(ComponentType.Task).get()
	private val renderableEntities = world.getEntitiesFor().all(ComponentType.Renderable).get()
	private val actionSequenceEntities = world.getEntitiesFor().all(ComponentType.ActionSequence).get()

	private val entitiesToProcess = Array<Entity>(false, 16)

	val minTurnTime = 0.1f
	var turnTimeAccumulator = 0f

	private fun getCanStartTurn(): Boolean
	{
		if (turnTimeAccumulator < minTurnTime) return false

		var canUpdate = true
		for (i in 0 until renderableEntities.entities.size)
		{
			val entity = renderableEntities.entities[i]
			val renderable = entity.renderable()!!

			if (renderable.renderable.animation?.isBlocking == true)
			{
				canUpdate = false
				break
			}
			else if (entity.transient()?.blocksTurns == true && renderable.renderable.isBlocking)
			{
				canUpdate = false
				break
			}
		}

		if (canUpdate)
		{
			for (i in 0 until actionSequenceEntities.entities.size)
			{
				val entity = actionSequenceEntities.entities[i]
				val sequence = entity.actionSequence()!!

				if (!sequence.actionSequenceState!!.blocked)
				{
					canUpdate = false
					break
				}
			}
		}

		return canUpdate
	}

	override fun doUpdate(deltaTime: Float)
	{
		turnTimeAccumulator += deltaTime

		if (entitiesToProcess.size > 0)
		{
			updateProcessList()
		}

		if (getCanStartTurn())
		{
			tryStartTurn()
		}
	}

	private fun tryStartTurn()
	{
		val actionAmount = getPlayerActionAmount()
		if (actionAmount > 0f)
		{
			for (i in 0 until taskEntities.entities.size)
			{
				val entity = taskEntities.entities[i]
				if (entity == world.player) continue

				val task = entity.task()!!
				task.actionAccumulator += actionAmount

				if (task.actionAccumulator > 0f)
				{
					entitiesToProcess.add(entity)
				}
			}

			updateProcessList()
			world.onTurn()

			turnTimeAccumulator = 0f
		}
	}

	private fun updateProcessList()
	{
		val itr = entitiesToProcess.iterator()
		while (itr.hasNext())
		{
			val entity = itr.next()
			val task = entity.task()!!

			val processState = processEntity(entity)
			if (processState != ProcessEntityState.SUCCESS || task.actionAccumulator <= 0f)
			{
				itr.remove()
			}
		}
	}

	protected fun processEntity(entity: Entity): ProcessEntityState
	{
		if (entity.isMarkedForDeletion()) return ProcessEntityState.SKIPPED
		if (world.player != null && entity.position()!!.position.taxiDist(world.player!!.position()!!.position) > 100) return ProcessEntityState.SKIPPED

		val task = entity.task()!!

		if (task.tasks.size == 0)
		{
			doEntityAI(entity)
		}

		if (task.tasks.size == 0)
		{
			task.actionAccumulator = 0f
			return ProcessEntityState.NO_TASK
		}

		val t = task.tasks.removeIndex(0)
		t.execute(entity, world, world.rng)
		entity.event()?.onTurn?.invoke()

		task.actionAccumulator -= getTaskCost(entity, t)

		t.free()

		return ProcessEntityState.SUCCESS
	}

	protected abstract fun getTaskCost(entity: Entity, task: AbstractTask): Float
	protected abstract fun getPlayerActionAmount(): Float
	protected abstract fun doEntityAI(entity: Entity)
}

enum class ProcessEntityState
{
	SKIPPED,
	NO_TASK,
	SUCCESS
}