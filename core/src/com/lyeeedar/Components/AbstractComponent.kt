package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader
abstract class AbstractComponentData : XmlDataClass()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	//endregion
}

abstract class AbstractComponent()
{
	abstract val type: ComponentType

	open fun onAddedToEntity(entity: Entity) { }
	open fun onRemovedFromEntity(entity: Entity) { }

	open fun toShortString(): String
	{
		return type.toString()
	}

	var obtained = false
	fun free()
	{
		ComponentPool.free(this)
	}

	abstract fun reset()
}

abstract class DataComponent : AbstractComponent()
{
	abstract fun initialiseFrom(data: AbstractComponentData)
}