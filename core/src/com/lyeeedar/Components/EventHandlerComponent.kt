package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.ActionSequence.ActionSequence
import com.lyeeedar.Systems.EventType
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import java.util.*

class EventAndCondition() : XmlDataClass()
{
	lateinit var condition: CompiledExpression
	lateinit var sequence: ActionSequence

	//region generated
	override fun load(xmlData: XmlData)
	{
		condition = CompiledExpression(xmlData.get("Condition"))
		val sequenceEl = xmlData.getChildByName("Sequence")!!
		sequence = ActionSequence()
		sequence.load(sequenceEl)
	}
	//endregion
}

class EventHandlerComponentData : AbstractComponentData()
{
	val handlers: FastEnumMap<EventType, Array<EventAndCondition>> = FastEnumMap(EventType::class.java)

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val handlersEl = xmlData.getChildByName("Handlers")
		if (handlersEl != null)
		{
			for (el in handlersEl.children)
			{
				val enumVal = EventType.valueOf(el.name.toUpperCase(Locale.ENGLISH))
				val objhandlers: Array<EventAndCondition> = Array()
				val objhandlersEl = xmlData.getChildByName("Handlers")
				if (objhandlersEl != null)
				{
					for (el in objhandlersEl.children)
					{
						val objobjhandlers: EventAndCondition
						val objobjhandlersEl = xmlData.getChildByName("Handlers")!!
						objobjhandlers = EventAndCondition()
						objobjhandlers.load(objobjhandlersEl)
						objhandlers.add(objobjhandlers)
					}
				}
				handlers[enumVal] = objhandlers
			}
		}
	}
	override val classID: String = "EventHandler"
	//endregion
}

inline fun Entity.eventHandler(): EventHandlerComponent? = this.components[ComponentType.EventHandler] as EventHandlerComponent?
class EventHandlerComponent(data: EventHandlerComponentData) : AbstractComponent<EventHandlerComponentData>(data)
{
	override val type: ComponentType = ComponentType.EventHandler

	override fun reset()
	{

	}
}