package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.occludes(): OccludesComponent? = this.components[ComponentType.Occludes] as OccludesComponent?
class OccludesComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Occludes

	var occludes = true

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		occludes = xml.getBoolean("Occludes", true)
	}
}