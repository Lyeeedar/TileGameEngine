package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.EnumBitflag
import com.lyeeedar.Util.Statics.Companion.collisionGrid
import squidpony.squidmath.LightRNG

class World(var grid: Array2D<AbstractTile>)
{
	lateinit var rng: LightRNG

	var tileSize: Float = 40f

	val entities = Array<Entity>(false, 128)
	private val toBeAdded = Array<Entity>(false, 128)
	private val toBeRemoved = ObjectSet<Entity>()

	var player: Entity? = null

	val systems = Array<AbstractSystem>()
	private val registeredSignatures = Array<EntitySignature>()

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
		updateCollisionGrid()

		for (i in 0 until systems.size)
		{
			systems[i].onTurn()
		}
	}

	fun updateCollisionGrid()
	{
		var collisionGrid = collisionGrid
		if (collisionGrid == null || collisionGrid.width != grid.width || collisionGrid.height != grid.height)
		{
			collisionGrid = Array2D(grid.width, grid.height) { x,y -> grid[x, y].getPassable(SpaceSlot.LIGHT, null) }
		}
		else
		{
			for (x in 0 until grid.width)
			{
				for (y in 0 until grid.height)
				{
					collisionGrid[x, y] = grid[x, y].getPassable(SpaceSlot.LIGHT, null)
				}
			}
		}
	}

	class EntitySignatureBuilder(val world: World)
	{
		private val all = EnumBitflag<ComponentType>()
		private val any = EnumBitflag<ComponentType>()

		fun all(vararg types: ComponentType): EntitySignatureBuilder
		{
			for (type in types)
			{
				this.all.setBit(type)
			}

			return this
		}

		fun any(vararg types: ComponentType): EntitySignatureBuilder
		{
			for (type in types)
			{
				this.any.setBit(type)
			}

			return this
		}

		fun get(): EntitySignature
		{
			val signature = EntitySignature(all, any)

			for (existing in world.registeredSignatures)
			{
				if (existing.isEqual(signature))
				{
					return existing
				}
			}

			world.registeredSignatures.add(signature)
			return signature
		}
	}
}