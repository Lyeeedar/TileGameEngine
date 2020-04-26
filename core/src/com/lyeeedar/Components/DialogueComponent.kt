package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

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
	var text: String = ""
	var turnsToShow: Int = -1

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		text = xmlData.get("Text", "")!!
		turnsToShow = xmlData.getInt("TurnsToShow", -1)
		afterLoad()
	}
	override val classID: String = "Dialogue"
	//endregion
}