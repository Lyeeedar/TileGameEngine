package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.name(): NameComponent = this.nameOrNull()!!
inline fun Entity.nameOrNull(): NameComponent? = this.components[ComponentType.Name] as NameComponent?
class NameComponent() : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Name

	lateinit var name: String
	lateinit var title: String

	fun set(name: String): NameComponent
	{
		this.name = name
		return this
	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		name = xml.get("Name", "")!!
		title = xml.get("Title", "")!!
	}

	override fun reset()
	{
		name = ""
		title = ""
	}
}