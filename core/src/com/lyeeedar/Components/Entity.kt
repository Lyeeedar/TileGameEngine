package com.lyeeedar.Components

import com.lyeeedar.Util.EnumBitflag
import com.lyeeedar.Util.FastEnumMap

class Entity
{
	val signature = EnumBitflag<ComponentType>()
	@JvmField val components = FastEnumMap<ComponentType, AbstractComponent>(ComponentType::class.java)

	fun addComponent(component: AbstractComponent)
	{
		components[component.type] = component
		signature.setBit(component.type)
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

		return component
	}

	var obtained = false
	fun free()
	{
		EntityPool.free(this)
	}
}