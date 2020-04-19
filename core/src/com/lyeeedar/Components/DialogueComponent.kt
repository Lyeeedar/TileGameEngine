package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.dialogue(): DialogueComponent? = this.components[ComponentType.Dialogue] as DialogueComponent?
class DialogueComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Dialogue

	var text: String = ""
	var displayedText: String = ""

	var textAccumulator: Float = 0f
	var textFade: Float = 0.5f

	var turnsToShow = -1

	val alpha: Float
		get() = textFade / 0.5f

	var remove: Boolean = false
		set(value)
		{
			field = value
			textFade = 0.5f
		}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{

	}

	override fun reset()
	{
		text = ""
		displayedText = ""
		textAccumulator = 0f
		textFade = 0.5f
		turnsToShow = -1
		remove = false
	}
}