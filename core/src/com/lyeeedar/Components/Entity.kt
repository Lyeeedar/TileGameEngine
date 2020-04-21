package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.*

class Entity
{
	val signature = EnumBitflag<ComponentType>()
	@JvmField val components = FastEnumMap<ComponentType, AbstractComponent<*>>(ComponentType::class.java)

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

	val components: Array<AbstractComponentData> = Array<AbstractComponentData>()

	//[generated]
	override fun load(xmlData: XmlData)
	{
		extends = xmlData.get("Extends", "")!!
		val componentsEl = xmlData.getChildByName("Components")
		if (componentsEl != null)
		{
			for (el in componentsEl.children)
			{
				val obj = AbstractComponentData.loadPolymorphicClass(el.get("classID"))
				obj.load(el)
				components.add(obj)
			}
		}
	}
	//[/generated]
}