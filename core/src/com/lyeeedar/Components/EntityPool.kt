package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.Pool
import ktx.collections.set

class EntityPool
{
	companion object
	{
		private val pool: Pool<Entity> = object : Pool<Entity>() {
			override fun newObject(): Entity
			{
				return Entity()
			}

		}

		@JvmStatic fun obtain(): Entity
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.obtained = true

			return obj
		}

		private val toBeFreed = Array<Entity>(false, 16)

		@JvmStatic fun free(entity: Entity)
		{
			toBeFreed.add(entity)
		}

		private fun flushFreedEntity(entity: Entity)
		{
			for (type in ComponentType.Values)
			{
				val component = entity.components[type]
				component?.free()
			}
			entity.components.clear()
			entity.signature.clear()

			entity.obtained = false
			pool.free(entity)
		}

		@JvmStatic fun flushFreedEntities()
		{
			for (entity in toBeFreed)
			{
				for (type in ComponentType.Temporary)
				{
					val component = entity.removeComponent(type)
					component?.free()
				}

				val builder = entity.archetypeBuilder()
				if (builder != null && builder.builder.requiredComponents.isContainedBy(entity.signature))
				{
					builder.builder.free(entity)
				}
				else
				{
					flushFreedEntity(entity)
				}
			}
			toBeFreed.clear()
		}
	}
}