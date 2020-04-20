package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.loadData(): LoadDataComponent? = this.components[ComponentType.LoadData] as LoadDataComponent?
class LoadDataComponent() : AbstractComponent<EmptyComponentData>(EmptyComponentData())
{
	override val type: ComponentType = ComponentType.LoadData

	lateinit var path: String
	lateinit var xml: XmlData
	var fromLoad = false

	fun set(path: String, xml: XmlData, fromLoad: Boolean): LoadDataComponent
	{
		this.path = path
		this.xml = xml
		this.fromLoad = fromLoad

		return this
	}

	override fun reset()
	{
		fromLoad = false
	}
}