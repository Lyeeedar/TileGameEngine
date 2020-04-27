package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Renderable")
class AttachParticleAction : AbstractDurationActionSequenceAction()
{
	val dataKey = "attachedParticles"

	lateinit var particle: ParticleEffectDescription
	lateinit var key: String

	var above: Boolean = true
	var killOnEnd: Boolean = false

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		val attachedParticles = Array<Entity>()
		for (target in state.targets)
		{
			val tile = state.world.grid.tryGet(target, null) ?: continue

			for (slot in SpaceSlot.EntityValues)
			{
				val entity = tile.contents[slot]?.get() ?: continue

				val addRenderable = entity.addOrGet(ComponentType.AdditionalRenderable) as AdditionalRenderableComponent

				val r = particle.getParticleEffect()

				if (above)
				{
					addRenderable.above[key] = r
				}
				else
				{
					addRenderable.below[key] = r
				}

				attachedParticles.add(entity)
			}
		}

		state.data[dataKey] = attachedParticles

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val attachedParticles = state.data[dataKey] as Array<Entity>
		for (entity in attachedParticles)
		{
			val addRenderable = entity.additionalRenderable() ?: continue

			val r: ParticleEffect
			if (above)
			{
				r = addRenderable.above[key] as ParticleEffect
				addRenderable.above.remove(key)
			}
			else
			{
				r = addRenderable.below[key] as ParticleEffect
				addRenderable.below.remove(key)
			}
			r.stop()

			if (!killOnEnd)
			{
				val newEntity = transientParticleArchetype.build()
				newEntity.position()!!.position = entity.position()!!.position
				newEntity.renderable()!!.renderable = r

				state.world.addEntity(entity)
			}
		}
		state.data.remove(dataKey)

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		key = xmlData.get("Key")
		above = xmlData.getBoolean("Above", true)
		killOnEnd = xmlData.getBoolean("KillOnEnd", false)
	}
	override val classID: String = "AttachParticle"
	//endregion
}