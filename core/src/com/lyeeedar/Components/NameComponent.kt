package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

class NameComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.Name

	lateinit var name: String
	lateinit var title: String

	override fun reset()
	{

	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as NameComponentData
		name = data.name
		title = data.title
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
	}
	override val classID: String = "Name"
	//endregion
}