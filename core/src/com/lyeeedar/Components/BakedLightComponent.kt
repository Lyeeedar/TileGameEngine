package com.lyeeedar.Components

import com.lyeeedar.Renderables.Light
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

class BakedLightComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.BakedLight

	lateinit var light: Light

	override fun reset()
	{
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as BakedLightComponentData
		light = data.light.copy()
	}
}

class BakedLightComponentData : AbstractComponentData()
{
	lateinit var light: Light

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val lightEl = xmlData.getChildByName("Light")!!
		light = Light()
		light.load(lightEl)
	}
	override val classID: String = "BakedLight"
	//endregion
}