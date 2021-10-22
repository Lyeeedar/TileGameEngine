package com.lyeeedar.Renderables.AnimGraph

import com.esotericsoftware.spine.attachments.Attachment
import com.lyeeedar.Renderables.AnimationGraphState
import com.lyeeedar.Renderables.Attachments.LightAttachment
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set

abstract class AbstractAttachmentAnimGraphAction : AbstractAnimGraphAction()
{
	override fun enter(state: AnimationGraphState)
	{
		attach(state)
	}

	protected abstract fun createAttachment(): Attachment

	private fun attach(state: AnimationGraphState)
	{
		val slot = state.getSlot(slot)
		val attachment = createAttachment()

		slot.attachment = attachment

		state.actionData[hashCode().toString()] = attachment
	}

	override fun update(delta: Float, state: AnimationGraphState)
	{
		val slot = state.getSlot(slot)
		if (slot.attachment == null)
		{
			attach(state)
		}
	}

	override fun exit(state: AnimationGraphState)
	{
		val slot = state.getSlot(slot)
		if (slot.attachment == state.actionData[hashCode().toString()])
		{
			slot.attachment = null
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	//endregion
}