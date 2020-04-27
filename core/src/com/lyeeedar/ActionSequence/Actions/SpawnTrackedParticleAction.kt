package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Renderable")
class SpawnTrackedParticleAction : AbstractDurationActionSequenceAction()
{
	val key = "spawnedParticles"

	lateinit var particle: ParticleEffectDescription
	var spawnSingleParticle: Boolean = false

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		val spawnedParticles = Array<Entity>()
		if (spawnSingleParticle)
		{
			val min = state.targets.minBy(Point::hashCode)!!
			val max = state.targets.maxBy(Point::hashCode)!!

			val entity = nonTransientParticleArchetype.build()

			val pos = entity.position()!!

			pos.min = min
			pos.max = max
			pos.slot = SpaceSlot.EFFECT

			val r = particle.getParticleEffect()
			r.size[0] = (pos.max.x - pos.min.x) + 1
			r.size[1] = (pos.max.y - pos.min.y) + 1

			entity.renderable()?.set(r)

			state.world.addEntity(entity)
			spawnedParticles.add(entity)
		}
		else
		{
			for (target in state.targets)
			{
				val entity = nonTransientParticleArchetype.build()

				val pos = entity.position()!!

				pos.slot = SpaceSlot.EFFECT

				val r = particle.getParticleEffect()
				entity.renderable()?.set(r)

				state.world.addEntity(entity)
				spawnedParticles.add(entity)
			}
		}
		state.data[key] = spawnedParticles

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		val spawnedParticles = state.data[key] as Array<Entity>
		for (entity in spawnedParticles)
		{
			val particle = entity.renderable()!!.renderable as ParticleEffect
			particle.stop()

			entity.addComponent(ComponentType.Transient)
		}
		state.data.remove(key)

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		spawnSingleParticle = xmlData.getBoolean("SpawnSingleParticle", false)
	}
	override val classID: String = "SpawnTrackedParticle"
	//endregion
}