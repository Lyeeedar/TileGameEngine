package com.lyeeedar.Systems

import com.lyeeedar.Components.Entity

abstract class AbstractEntitySystem(world: World, val entitySignature: EntitySignature) : AbstractSystem(world)
{
	override fun doUpdate(deltaTime: Float)
	{
		beforeUpdate(deltaTime)
		for (i in 0 until world.entities.size)
		{
			val entity = world.entities[i]
			if (entitySignature.matches(entity))
			{
				updateEntity(entity, deltaTime)
			}
		}
		afterUpdate(deltaTime)
	}
	abstract fun updateEntity(entity: Entity, deltaTime: Float)
	open fun beforeUpdate(deltaTime: Float) {}
	open fun afterUpdate(deltaTime: Float) {}

	override fun onTurn()
	{
		beforeOnTurn()
		for (i in 0 until world.entities.size)
		{
			val entity = world.entities[i]
			if (entitySignature.matches(entity))
			{
				onTurnEntity(entity)
			}
		}
		afterOnTurn()
	}
	abstract fun onTurnEntity(entity: Entity)
	open fun beforeOnTurn() {}
	open fun afterOnTurn() {}
}