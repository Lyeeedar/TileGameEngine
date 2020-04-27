package com.lyeeedar.Components

import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.XmlData

class EventComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Event

	val onTurn = Event0Arg()

	override fun reset()
	{
		onTurn.clear()
	}
}