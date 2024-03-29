package com.lyeeedar.Systems

import com.lyeeedar.Components.*

class ActionSequenceSystem(world: World<*>) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.ActionSequence).get())
{
	override fun beforeUpdate(deltaTime: Float)
	{
		for (i in 0 until entitySignature.entities.size)
		{
			val entity = entitySignature.entities[i]
			val actionSequence = entity.actionSequence()!!

			actionSequence.actionSequenceState.cachedDelayedState = actionSequence.actionSequence.isDelayed(actionSequence.actionSequenceState)
		}
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val actionSequence = entity.actionSequence()!!
		var complete = actionSequence.actionSequence.update(deltaTime, actionSequence.actionSequenceState)

		if (!actionSequence.actionSequenceState.source.isValid())
		{
			actionSequence.actionSequence.cancel(actionSequence.actionSequenceState)
			complete = true
		}

		if (complete)
		{
			if (entity.isTransient())
			{
				entity.markForDeletion(0f, "completed")
			}
			else
			{
				val comp = entity.removeComponent(ComponentType.ActionSequence)
				comp?.free()
			}
		}
	}

	override fun onTurnEntity(entity: Entity)
	{
		val actionSequence = entity.actionSequence()!!
		actionSequence.actionSequence.onTurn(actionSequence.actionSequenceState)
	}
}