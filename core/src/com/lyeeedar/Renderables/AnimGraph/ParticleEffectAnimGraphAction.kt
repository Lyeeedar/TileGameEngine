package com.lyeeedar.Renderables.AnimGraph

import com.esotericsoftware.spine.attachments.Attachment
import com.lyeeedar.Renderables.Attachments.RenderableAttachment
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

class ParticleEffectAnimGraphAction : AbstractAttachmentAnimGraphAction()
{
	lateinit var particleEffectDescription: ParticleEffectDescription

	override fun createAttachment(): Attachment
	{
		return RenderableAttachment(particleEffectDescription.getParticleEffect(), "ParticleEffectAnimGraphAction")
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particleEffectDescription = AssetManager.loadParticleEffect(xmlData.getChildByName("ParticleEffectDescription")!!)
	}
	override val classID: String = "ParticleEffectAnimGraph"
	//endregion
}