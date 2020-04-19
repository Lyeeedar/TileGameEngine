package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.EnumBitflag
import com.lyeeedar.Util.XmlData

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

			for (type in ComponentType.Values)
			{
				entity.components[type]?.reset()
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

fun Entity.archetypeBuilder(): ArchetypeBuilderComponent? = this.components[ComponentType.ArchetypeBuilder] as ArchetypeBuilderComponent?
class ArchetypeBuilderComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.ArchetypeBuilder

	lateinit var builder: EntityArchetypeBuilder

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}