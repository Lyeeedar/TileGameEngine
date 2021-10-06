package com.lyeeedar.Components

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

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

	open fun serialize(kryo: Kryo, output: Output) { }
	open fun deserialize(kryo: Kryo, input: Input) { }

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

expect enum class ComponentType