package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.Systems.EventType
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlDataClass

class EventAndCondition() : XmlDataClass()
{
	lateinit var condition: CompiledExpression
	lateinit var sequence: ActionSequence
}

class EventHandlerComponentData : AbstractComponentData()
{
	val handlers: FastEnumMap<EventType, Array<EventAndCondition>> = FastEnumMap(EventType::class.java)
}

inline fun Entity.eventHandler(): EventHandlerComponent? = this.components[ComponentType.EventHandler] as EventHandlerComponent?
class EventHandlerComponent(data: EventHandlerComponentData) : AbstractComponent<EventHandlerComponentData>(data)
{
	override val type: ComponentType = ComponentType.EventHandler

	override fun reset()
	{

	}
}