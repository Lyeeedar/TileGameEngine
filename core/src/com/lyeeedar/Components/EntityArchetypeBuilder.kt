package com.lyeeedar.Components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.EnumBitflag
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.min

class EntityArchetypeBuilder
{
	private val entityPool = Array<Entity>(false, 16)

	val requiredComponents = EnumBitflag<ComponentType>()

	fun add(componentType: ComponentType): EntityArchetypeBuilder
	{
		if (requiredComponents.contains(componentType)) throw RuntimeException("Builder already contains component '$componentType'")

		requiredComponents.setBit(componentType)
		return this
	}

	fun build(): Entity
	{
		val entity: Entity
		if (entityPool.size > 0)
		{
			entity = entityPool.pop()
			if (entity.obtained) throw RuntimeException()
			entity.obtained = true
			entity.usageID++

			for (type in ComponentType.Values)
			{
				entity.components[type.ordinal]?.reset()
			}
		}
		else
		{
			entity = EntityPool.obtain()

			for (type in ComponentType.Values)
			{
				if (requiredComponents.contains(type)) {
					entity.addComponent(ComponentPool.obtain(type))
				}
			}
		}

		val builderComponent = ComponentPool.obtain(ComponentType.ArchetypeBuilder) as ArchetypeBuilderComponent
		builderComponent.builder = this
		entity.addComponent(builderComponent)

		return entity
	}

	fun free(entity: Entity)
	{
		if (!entity.obtained) throw RuntimeException()
		entity.obtained = false
		entity.usageID++

		for (type in ComponentType.Values)
		{
			if (!requiredComponents.contains(type))
			{
				entity.removeComponent(type)?.free()
			}
		}

		entityPool.add(entity)
	}
}

class ArchetypeBuilderComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.ArchetypeBuilder

	lateinit var builder: EntityArchetypeBuilder

	override fun reset() {}
}

val transientParticleArchetype =
	EntityArchetypeBuilder()
		.add(ComponentType.Position)
		.add(ComponentType.Renderable)
		.add(ComponentType.Transient)

val transientActionSequenceArchetype =
	EntityArchetypeBuilder()
		.add(ComponentType.ActionSequence)
		.add(ComponentType.Transient)

val nonTransientParticleArchetype =
	EntityArchetypeBuilder()
		.add(ComponentType.Position)
		.add(ComponentType.Renderable)

fun Renderable.addToWorld(world: World<*>, point: Point, offset: Vector2 = Vector2(), slot: SpaceSlot = SpaceSlot.EFFECT, isBlocking: Boolean = true): Entity
{
	val pe = transientParticleArchetype.build()
	pe.renderable()!!.renderable = this
	pe.transient()!!.blocksTurns = isBlocking

	val ppos = pe.position()!!
	ppos.slot = slot

	ppos.size = min(this.size[0], this.size[1])
	ppos.position = point
	ppos.offset = offset

	world.addEntity(pe)
	return pe
}