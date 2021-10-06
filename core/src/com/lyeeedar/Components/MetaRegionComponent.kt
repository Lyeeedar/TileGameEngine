package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.XmlData

class MetaRegionComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.MetaRegion

	val keys: Array<String> = Array<String>()

	override fun reset()
	{
		keys.clear()
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as MetaRegionComponentData

		keys.addAll(data.keys)
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
	}
	override val classID: String = "MetaRegion"
	//endregion
}