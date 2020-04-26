package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.addAll

class MetaRegionComponent(data: MetaRegionComponentData) : AbstractComponent<MetaRegionComponentData>(data)
{
	override val type: ComponentType = ComponentType.MetaRegion

	override fun reset()
	{

	}
}

class MetaRegionComponentData : AbstractComponentData()
{
	val keys: Array<String> = Array<String>()

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val keysEl = xmlData.getChildByName("Keys")
		if (keysEl != null)
		{
			for (el in keysEl.children)
			{
				keys.add(el.text)
			}
		}
		afterLoad()
	}
	override val classID: String = "MetaRegion"
	//endregion
}