package com.lyeeedar.Renderables.AnimGraph

import com.esotericsoftware.spine.attachments.Attachment
import com.lyeeedar.Renderables.Attachments.LightAttachment
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Util.XmlData

class LightAnimGraphAction : AbstractAttachmentAnimGraphAction()
{
	lateinit var light: Light
	override fun createAttachment(): Attachment
	{
		return LightAttachment(light.copy(), "LightAnimGraphAction")
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val lightEl = xmlData.getChildByName("Light")!!
		light = Light()
		light.load(lightEl)
	}
	override val classID: String = "LightAnimGraph"
	//endregion
}