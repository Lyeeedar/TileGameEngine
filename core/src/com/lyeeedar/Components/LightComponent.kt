package com.lyeeedar.Components

import com.lyeeedar.Renderables.Light
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

class LightComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.Light

	lateinit var light: Light

	override fun reset()
	{
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as LightComponentData
		light = data.light.copy()
	}
}

@DataClass(name = "LightComponent")
class LightComponentData : AbstractComponentData()
{
	lateinit var light: Light

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		light = AssetManager.loadLight(xmlData.getChildByName("Light")!!)
	}
	override val classID: String = "Light"
	//endregion
}