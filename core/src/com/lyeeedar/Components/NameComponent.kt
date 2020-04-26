package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

class NameComponent(data: NameComponentData) : AbstractComponent<NameComponentData>(data)
{
	override val type: ComponentType = ComponentType.Name

	override fun reset()
	{

	}
}

class NameComponentData : AbstractComponentData()
{
	lateinit var name: String
	lateinit var title: String

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		name = xmlData.get("Name")
		title = xmlData.get("Title")
		afterLoad()
	}
	override val classID: String = "Name"
	//endregion
}