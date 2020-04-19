package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.XmlData
import ktx.collections.addAll
import java.util.*

inline fun Entity.metaRegion(): MetaRegionComponent? = this.components[ComponentType.MetaRegion] as MetaRegionComponent?
class MetaRegionComponent :  AbstractComponent()
{
	override val type: ComponentType = ComponentType.MetaRegion

	val keys = Array<String>(1)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		keys.addAll(xml.get("Key").toLowerCase(Locale.ENGLISH).split(','))
	}
}
