package com.lyeeedar.Renderables.Attachments

import com.esotericsoftware.spine.attachments.Attachment
import com.lyeeedar.Renderables.Renderable

class RenderableAttachment(var renderable: Renderable, name: String) : Attachment(name)
{
	override fun copy(): Attachment
	{
		return RenderableAttachment(renderable.copy(), name)
	}
}