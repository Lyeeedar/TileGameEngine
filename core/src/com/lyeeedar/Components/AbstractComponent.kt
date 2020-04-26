package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataClassLoader
abstract class AbstractComponentData : XmlDataClass()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
		afterLoad()
	}
	abstract val classID: String
	//endregion
}
class EmptyComponentData : AbstractComponentData()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		afterLoad()
	}
	override val classID: String = "Empty"
	//endregion
}
abstract class AbstractComponent<T: AbstractComponentData>(var data: T)
{
	abstract val type: ComponentType

	fun swapData(data: AbstractComponentData)
	{
		this.data = data as T
		onDataSwapped()
	}
	open fun onDataSwapped() {}

	var obtained = false
	fun free()
	{
		ComponentPool.free(this)
	}

	abstract fun reset()
}

abstract class NonDataComponent : AbstractComponent<EmptyComponentData>(EmptyComponentData())