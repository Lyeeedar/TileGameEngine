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

	override fun enter(state: ActionSequenceState)
	{
		val attachedParticles = Array<EntityReference>()
		for (target in state.targets)
		{
			val tile = state.world.grid.tryGet(target, null) ?: continue

			for (slot in SpaceSlot.EntityValues)
			{
				val entityRef = tile.contents[slot] ?: continue
				val entity = entityRef.get() ?: continue

				val addRenderable = entity.addOrGet(ComponentType.AdditionalRenderable) as AdditionalRenderableComponent

				val r = particle.getParticleEffect()

				if (above)
				{
					addRenderable.above[key+state.uid] = r
				}
				else
				{
					addRenderable.below[key+state.uid] = r
				}

				attachedParticles.add(entityRef)
			}
		}

		state.data[dataKey] = attachedParticles
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val attachedParticles = state.data[dataKey] as Array<EntityReference>
		for (entityRef in attachedParticles)
		{
			val entity = entityRef.get() ?: continue
			val addRenderable = entity.additionalRenderable() ?: continue

			val r: ParticleEffect
			if (above)
			{
				r = addRenderable.above[key+state.uid] as ParticleEffect
				addRenderable.above.remove(key+state.uid)
			}
			else
			{
				r = addRenderable.below[key+state.uid] as ParticleEffect
				addRenderable.below.remove(key+state.uid)
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