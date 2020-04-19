package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.isTransient() = this.transient() != null
inline fun Entity.transient(): TransientComponent? = this.components[ComponentType.Transient] as TransientComponent?
class TransientComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Transient

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}
}