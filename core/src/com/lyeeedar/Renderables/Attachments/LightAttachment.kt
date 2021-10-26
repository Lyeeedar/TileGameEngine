package com.lyeeedar.Renderables.Attachments

import com.esotericsoftware.spine.attachments.Attachment
import com.lyeeedar.Renderables.Light

class LightAttachment(val light: Light, name: String) : Attachment(name)
{
	override fun copy(): Attachment
	{
		return LightAttachment(light.copy(), name)
	}
}