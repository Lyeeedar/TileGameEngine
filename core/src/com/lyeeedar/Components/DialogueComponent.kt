package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.dialogue(): DialogueComponent? = this.components[ComponentType.Dialogue] as DialogueComponent?
class DialogueComponent(data: DialogueComponentData) : AbstractComponent<DialogueComponentData>(data)
{
	override val type: ComponentType = ComponentType.Dialogue

	var remainingTurnsToShow = -1

	var displayedText: String = ""

	var textAccumulator: Float = 0f
	var textFade: Float = 0.5f

	val alpha: Float
		get() = textFade / 0.5f

	var remove: Boolean = false
		set(value)
		{
			field = value
			textFade = 0.5f
		}

	override fun reset()
	{
		remainingTurnsToShow = data.turnsToShow
		displayedText = ""
		textAccumulator = 0f
		remove = false
	}
}

class DialogueComponentData : AbstractComponentData()
{
	override val classID: String = "Dialogue"

	var text: String = ""
	var turnsToShow = -1

	//[generated]
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		text = xmlData.get("Text", "")!!
	}
	//[/generated]
}