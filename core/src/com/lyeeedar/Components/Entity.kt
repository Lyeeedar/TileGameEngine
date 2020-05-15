package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

class EntityReference private constructor(val entity: Entity)
{
	val id = entity.usageID

	fun isValid(): Boolean
	{
		if (!entity.obtained) return false
		if (entity.usageID != id) return false

		return true
	}

	fun get(): Entity?
	{
		if (isValid()) return entity

		return null
	}

	override fun hashCode(): Int
	{
		return entity.hashCode() + id
	}

	override fun equals(other: Any?): Boolean
	{
		if (other is Entity)
		{
			return other == entity && other.usageID == id
		}
		else if (other is EntityReference)
		{
			return other.hashCode() == hashCode()
		}
		else
		{
			return false
		}
	}

	override fun toString(): String
	{
		return if (isValid()) "ref:${entity}" else "ref:invalid"
	}

	companion object
	{
		fun create(entity: Entity): EntityReference
		{
			return EntityReference(entity)
		}
	}
}

class Entity
{
	var world: World<*>? = null

	val signature = EnumBitflag<ComponentType>()
	@JvmField val components = FastEnumMap<ComponentType, AbstractComponent>(ComponentType::class.java)

	fun addOrGet(componentType: ComponentType): AbstractComponent
	{
		var comp = components[componentType]
		if (comp == null)
		{
			comp = addComponent(componentType)
		}

		return comp
	}

	fun addComponent(component: AbstractComponent)
	{
		components[component.type] = component
		signature.setBit(component.type)

		component.onAddedToEntity(this)

		world?.entityListsDirty = true
	}

	fun addComponent(componentType: ComponentType): AbstractComponent
	{
		val component = ComponentPool.obtain(componentType)
		addComponent(component)

		return component
	}

	fun removeComponent(componentType: ComponentType): AbstractComponent?
	{
		val component = components[componentType]
		components.remove(componentType)
		signature.clearBit(componentType)

		component?.onRemovedFromEntity(this)

		world?.entityListsDirty = true

		return component
	}

	fun hasComponent(componentType: ComponentType) = this.signature.contains(componentType)

	var usageID = 0
	var obtained = false
	fun free()
	{
		usageID++
		EntityPool.free(this)
	}

	private var currentRef: EntityReference? = null
	fun getRef(): EntityReference
	{
		if (currentRef != null && currentRef!!.isValid()) return currentRef!!

		currentRef = EntityReference.create(this)

		return currentRef!!
	}

	override fun toString(): String
	{
		val name = name()?.name ?: "Entity"

		val output = StringBuilder()
		output.append(name, ":")

		for (component in components)
		{
			output.append(component.toShortString()).append(",")
		}

		return output.toString()
	}
}

@DataFile(colour = "84,186,214", icon = "Sprites/player_1.png")
@DataClass(name = "Entity")
class EntityData : XmlDataClass()
{
	@DataFileReference(resourceType = "Entity")
	var extends: String = ""

	@DataArray(childrenAreUnique = true)
	val components: Array<AbstractComponentData> = Array<AbstractComponentData>()

	fun create(): Entity
	{
		return EntityLoader.load(this)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		extends = xmlData.get("Extends", "")!!
		val componentsEl = xmlData.getChildByName("Components")
		if (componentsEl != null)
		{
			for (el in componentsEl.children)
			{
				val objcomponents: AbstractComponentData
				val objcomponentsEl = el
				objcomponents = XmlDataClassLoader.loadAbstractComponentData(objcomponentsEl.get("classID", objcomponentsEl.name)!!)
				objcomponents.load(objcomponentsEl)
				components.add(objcomponents)
			}
		}
	}
	//endregion
}