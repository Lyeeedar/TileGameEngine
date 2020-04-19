package com.lyeeedar.Components

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Util.XmlData

abstract class AbstractComponent
{
	abstract val type: ComponentType

	abstract fun parse(xml: XmlData, entity: Entity, parentPath: String)

	open fun saveData(kryo: Kryo, output: Output)
	{

	}

	open fun loadData(kryo: Kryo, input: Input)
	{

	}

	open fun reset(){}

	var obtained = false
	fun free()
	{
		ComponentPool.free(this)
	}
}