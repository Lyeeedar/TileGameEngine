package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

class Entity
{
	var world: World? = null

	val signature = EnumBitflag<ComponentType>()
	@JvmField val components = FastEnumMap<ComponentType, AbstractComponent<*>>(ComponentType::class.java)

	fun addOrGet(componentType: ComponentType): AbstractComponent<*>
	{
		var comp = components[componentType]
		if (comp == null)
		{
			comp = addComponent(componentType)
		}

		return comp
	}

	fun addComponent(component: AbstractComponent<*>)
	{
		components[component.type] = component
		signature.setBit(component.type)
	}

	fun addComponent(componentType: ComponentType): AbstractComponent<*>
	{
		val component = ComponentPool.obtain(componentType)
		addComponent(component)

		return component
	}

	fun removeComponent(componentType: ComponentType): AbstractComponent<*>?
	{
		val component = components[componentType]
		components.remove(componentType)
		signature.clearBit(componentType)

		return component
	}

	fun hasComponent(componentType: ComponentType) = this.signature.contains(componentType)

	var obtained = false
	fun free()
	{
		EntityPool.free(this)
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
				objcomponents = XmlDataClassLoader.loadAbstractComponentData(objcomponentsEl.get("classID"))
				objcomponents.load(objcomponentsEl)
				components.add(objcomponents)
			}
		}
	}
	//endregion
}