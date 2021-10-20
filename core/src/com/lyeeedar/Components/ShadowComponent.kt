package com.lyeeedar.Components

import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Shadow
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

class ShadowComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.Shadow

	lateinit var shadow: Shadow

	override fun reset()
	{
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as ShadowComponentData
		shadow = data.shadow.copy()
	}
}

@DataClass(name = "ShadowComponent")
class ShadowComponentData : AbstractComponentData()
{
	lateinit var shadow: Shadow

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val shadowEl = xmlData.getChildByName("Shadow")!!
		shadow = Shadow()
		shadow.load(shadowEl)
	}
	override val classID: String = "Shadow"
	//endregion
}