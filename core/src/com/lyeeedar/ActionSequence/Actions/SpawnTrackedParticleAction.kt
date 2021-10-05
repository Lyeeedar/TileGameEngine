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

@DataClass(category = "Renderable", name = "TrackedFX")
class SpawnTrackedParticleAction : AbstractDurationActionSequenceAction()
{
	val key = "spawnedParticles"

	lateinit var particle: ParticleEffectDescription
	var spawnSingleParticle: Boolean = false
	var alignToVector: Boolean = true

	override fun enter(state: ActionSequenceState)
	{
		if (state.targets.size == 0) return

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

			if (alignToVector)
			{
				pos.facing = state.facing
			}

			val r = particle.getParticleEffect()
			r.size[0] = (pos.max.x - pos.min.x) + 1
			r.size[1] = (pos.max.y - pos.min.y) + 1

			if (alignToVector && r.useFacing)
			{
				r.rotation = pos.facing.angle
				r.facing = pos.facing

				if (pos.facing.x != 0)
				{
					val temp = r.size[0]
					r.size[0] = r.size[1]
					r.size[1] = temp
				}
			}

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

				pos.position = target
				pos.slot = SpaceSlot.EFFECT

				if (alignToVector)
				{
					pos.facing = state.facing
				}

				val r = particle.getParticleEffect()
				entity.renderable()?.set(r)

				state.world.addEntity(entity)
				spawnedParticles.add(entity)
			}
		}
		state.data[key] = spawnedParticles
	}

	override fun exit(state: ActionSequenceState)
	{
		val spawnedParticles = state.data[key] as Array<Entity>
		for (entity in spawnedParticles)
		{
			val particle = entity.renderable()!!.renderable as ParticleEffect
			particle.stop()

			entity.addComponent(ComponentType.Transient)
		}
		state.data.remove(key)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		spawnSingleParticle = xmlData.getBoolean("SpawnSingleParticle", false)
		alignToVector = xmlData.getBoolean("AlignToVector", true)
	}
	override val classID: String = "SpawnTrackedParticle"
	//endregion
}