package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.name(): NameComponent = this.nameOrNull()!!
inline fun Entity.nameOrNull(): NameComponent? = this.components[ComponentType.Name] as NameComponent?
class NameComponent(data: NameComponentData) : AbstractComponent<NameComponentData>(data)
{
	override val type: ComponentType = ComponentType.Name

	override fun reset()
	{

	}
}

class NameComponentData : AbstractComponentData()
{
	override val classID: String = "Name"

	lateinit var name: String
	lateinit var title: String

	//[generated]
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		name = xmlData.get("Name")
		title = xmlData.get("Title")
	}
	//[/generated]
}