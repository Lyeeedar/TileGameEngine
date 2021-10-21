package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import squidpony.squidmath.LightRNG

class World<T: AbstractTile>(var grid: Array2D<T>)
{
	lateinit var rng: LightRNG

	var tileSize: Float = 40f
	var ambientLight: Colour = Colour.WHITE.copy()
	var timeMultiplier = 1f
	var hitStop = 0f

	var entityListsDirty = true
	val entities = Array<Entity>(false, 128)
	private val toBeAdded = Array<Entity>(false, 128)
	private val toBeRemoved = ObjectSet<Entity>()

	var player: Entity? = null

	val systems = Array<AbstractSystem>()
	private val registeredSignatures = Array<EntitySignature>()

	private var doneFirstUpdate = false

	val onTurnEvent = Event0Arg()

	fun getEntitiesFor(): EntitySignatureBuilder
	{
		return EntitySignatureBuilder(this)
	}

	fun addEntity(entity: Entity)
	{
		if (entity.world != null) throw RuntimeException("Tried to add entity to world more than once!")

		toBeAdded.add(entity)

		entityListsDirty = true
	}

	fun removeEntity(entity: Entity)
	{
		toBeRemoved.add(entity)

		entityListsDirty = true
	}

	private fun updateEntityList()
	{
		if (!entityListsDirty) return
		entityListsDirty = false

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
			toBeAdded[i].world = this
			entities.add(toBeAdded[i])
		}

		toBeAdded.clear()
		toBeRemoved.clear()

		updateEntitySignatures()
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
		var timeMultiplier = timeMultiplier
		if (hitStop > 0f)
		{
			hitStop -= delta
			timeMultiplier *= 0.1f
		}

		val delta = delta * timeMultiplier

		updateEntityList()

		if (!doneFirstUpdate)
		{
			doneFirstUpdate = true
			updateCollisionGrid()
		}

		for (i in 0 until systems.size)
		{
			systems[i].update(delta)
		}

		EntityPool.flushFreedEntities()
	}

	fun onTurn()
	{
		updateCollisionGrid()

		for (i in 0 until systems.size)
		{
			systems[i].onTurn()
		}

		onTurnEvent.invoke()
	}

	fun updateCollisionGrid()
	{
		if (Statics.lightCollisionGrid == null || Statics.lightCollisionGrid!!.width != grid.width || Statics.lightCollisionGrid!!.height != grid.height)
		{
			Statics.lightCollisionGrid = Array2D(grid.width, grid.height) { x, y -> !grid[x, y].getPassable(SpaceSlot.LIGHT, null) }
		}
		else
		{
			for (x in 0 until grid.width)
			{
				for (y in 0 until grid.height)
				{
					Statics.lightCollisionGrid!![x, y] = !grid[x, y].getPassable(SpaceSlot.LIGHT, null)
				}
			}
		}
	}

	fun free()
	{
		onTurnEvent.clear()
		for (entity in entities)
		{
			entity.world = null
			entity.free()
		}
		entities.clear()

		for (system in systems)
		{
			system.free()
		}
		systems.clear()

		EntityPool.flushFreedEntities()
	}

	class EntitySignatureBuilder(val world: World<*>)
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