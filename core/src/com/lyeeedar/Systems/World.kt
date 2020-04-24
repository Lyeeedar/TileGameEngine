package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.Array2D
import squidpony.squidmath.LightRNG

class World
{
	lateinit var rng: LightRNG

	var tileSize: Float = 40f

	val entities = Array<Entity>(false, 128)
	val toBeAdded = Array<Entity>(false, 128)
	val toBeRemoved = ObjectSet<Entity>()

	var player: Entity? = null

	val systems = Array<AbstractSystem>()
	val registeredSignatures = Array<EntitySignature>()

	var grid: Array2D<Tile> = Array2D(0, 0) { x, y -> Tile(x, y) }

	fun getEntitiesFor(): EntitySignatureBuilder
	{
		return EntitySignatureBuilder(this)
	}

	fun addEntity(entity: Entity)
	{
		if (entity.world != null) throw RuntimeException("Tried to add entity to world more than once!")

		toBeAdded.add(entity)
	}

	fun removeEntity(entity: Entity)
	{
		toBeRemoved.add(entity)
	}

	private fun updateEntityList()
	{
		var i = 0
		while (i < entities.size)
		{
			if (toBeRemoved.contains(entities[i]))
			{
				entities[i].world = null
				entities.removeIndex(i)
				i--
			}

			i++
		}

		for (i in 0 until toBeAdded.size)
		{
			entities.add(toBeAdded[i])
		}

		toBeAdded.clear()
		toBeRemoved.clear()
	}

	private fun updateEntitySignatures()
	{
		for (i in 0 until registeredSignatures.size)
		{
			registeredSignatures[i].entities.clear()
		}
		for (i in 0 until entities.size)
		{
			val entity = entities[i]
			for (i in 0 until registeredSignatures.size)
			{
				val signature = registeredSignatures[i]
				if (signature.matches(entity))
				{
					signature.entities.add(entity)
				}
			}
		}
	}

	fun update(delta: Float)
	{
		for (i in 0 until systems.size)
		{
			systems[i].update(delta)
		}

		updateEntityList()
		updateEntitySignatures()
		EntityPool.flushFreedEntities()
	}

	fun onTurn()
	{
		for (i in 0 until systems.size)
		{
			systems[i].onTurn()
		}
	}
}