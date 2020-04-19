package com.lyeeedar.Components

import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.XmlData

fun Entity.event(): EventComponent? {
	var event = this.components[ComponentType.Event] as EventComponent?
	if (event == null)
	{
		event = this.addComponent(ComponentType.Event) as EventComponent
	}

	return event
}
class EventComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Event

	val onTurn = Event0Arg()
	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}
