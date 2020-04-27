package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Renderable")
class SpawnOneShotParticleAction : AbstractOneShotActionSequenceAction()
{
	enum class SpawnBehaviour
	{
		IMMEDIATE,
		FROM_SOURCE,
		FROM_CENTER,
		RANDOM
	}

	lateinit var particle: ParticleEffectDescription

	var alignToVector: Boolean = true
	var spawnBehaviour: SpawnBehaviour = SpawnBehaviour.IMMEDIATE
	var spawnDuration: Float = 0f

	override fun enter(state: ActionSequenceState): ActionState
	{
		val sourceTile = state.sourcePoint

		if (state.targets.size == 0) return ActionState.Completed

		val min = state.targets.minBy(Point::hashCode)!!
		val max = state.targets.maxBy(Point::hashCode)!!
		val furthest = state.targets.maxBy { it.taxiDist(sourceTile) }!!

		for (point in state.targets)
		{
			val tile = state.world.grid.tryGet(point, null) ?: continue

			val entity = transientParticleArchetype.build()

			val r = particle.getParticleEffect()
			entity.renderable()?.set(r)

			val renderDelay: Float
			if (spawnBehaviour == SpawnBehaviour.IMMEDIATE)
			{
				renderDelay = 0f
			}
			else if (spawnBehaviour == SpawnBehaviour.FROM_SOURCE)
			{
				val maxDist = furthest.euclideanDist(sourceTile)
				val dist = tile.euclideanDist(sourceTile)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				renderDelay = delay
			}
			else if (spawnBehaviour == SpawnBehaviour.FROM_CENTER)
			{
				val center = min.lerp(max, 0.5f)
				val maxDist = center.euclideanDist(max)
				val dist = center.euclideanDist(tile)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				renderDelay = delay
			}
			else if (spawnBehaviour == SpawnBehaviour.RANDOM)
			{
				val alpha = Random.random(Random.sharedRandom)
				val delay = spawnDuration * alpha

				renderDelay = delay
			}
			else
			{
				throw Exception("Unhandled spawn behaviour")
			}
			r.renderDelay = renderDelay

			val pos = entity.position()!!

			pos.position = tile
			pos.slot = SpaceSlot.EFFECT

			if (alignToVector)
			{
				pos.facing = state.facing
			}

			state.world.addEntity(entity)
		}

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		alignToVector = xmlData.getBoolean("AlignToVector", true)
		spawnBehaviour = SpawnBehaviour.valueOf(xmlData.get("SpawnBehaviour", SpawnBehaviour.IMMEDIATE.toString())!!.toUpperCase(Locale.ENGLISH))
		spawnDuration = xmlData.getFloat("SpawnDuration", 0f)
	}
	override val classID: String = "SpawnOneShotParticle"
	//endregion
}