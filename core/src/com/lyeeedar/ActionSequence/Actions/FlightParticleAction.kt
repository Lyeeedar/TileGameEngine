package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.math.Interpolation
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.transientParticleArchetype
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.UnsmoothedPath
import com.lyeeedar.Util.XmlData

@DataClass(category = "Renderable")
class FlightParticleAction : AbstractDurationActionSequenceAction()
{
	enum class SpawnBehaviour
	{
		IMMEDIATE,
		FROMSOURCE,
		FROMCENTER,
		RANDOM
	}

	lateinit var particle: ParticleEffectDescription
	var useLeap: Boolean = false

	var alignToVector: Boolean = true
	var spawnBehaviour: SpawnOneShotParticleAction.SpawnBehaviour = SpawnOneShotParticleAction.SpawnBehaviour.IMMEDIATE
	var spawnDuration: Float = 0f

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		val sourceTile = state.source.pos()!!.position

		val min = state.targets.minBy(Point::hashCode)!!
		val max = state.targets.maxBy(Point::hashCode)!!
		val midPoint = min + (max - min) / 2
		val furthest = state.targets.maxBy { it.taxiDist(sourceTile) }!!

		for (point in state.targets)
		{
			val tile = state.world.grid.tryGet(point, null) ?: continue

			val entity = transientParticleArchetype.build()

			val r = particle.getParticleEffect()
			entity.renderable()!!.data.renderable = r

			val renderDelay: Float
			if (spawnBehaviour == SpawnOneShotParticleAction.SpawnBehaviour.IMMEDIATE)
			{
				renderDelay = 0f
			}
			else if (spawnBehaviour == SpawnOneShotParticleAction.SpawnBehaviour.FROMSOURCE)
			{
				val maxDist = furthest.euclideanDist(sourceTile)
				val dist = tile.euclideanDist(sourceTile)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				renderDelay = delay
			}
			else if (spawnBehaviour == SpawnOneShotParticleAction.SpawnBehaviour.FROMCENTER)
			{
				val center = min.lerp(max, 0.5f)
				val maxDist = center.euclideanDist(max)
				val dist = center.euclideanDist(tile)
				val alpha = dist / maxDist
				val delay = spawnDuration * alpha

				renderDelay = delay
			}
			else if (spawnBehaviour == SpawnOneShotParticleAction.SpawnBehaviour.RANDOM)
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

			val pos = entity.pos()!!

			pos.position = tile
			pos.data.slot = SpaceSlot.EFFECT

			if (alignToVector)
			{
				pos.facing = state.facing
			}

			if (useLeap)
			{
				r.animation = LeapAnimation.obtain().set(duration, pos.position.getPosDiff(sourceTile), 2f)
				r.animation = ExpandAnimation.obtain().set(duration, 0.5f, 1.5f, false)
			}
			else
			{
				r.animation = MoveAnimation.obtain().set(duration, UnsmoothedPath(midPoint.getPosDiff(sourceTile)), Interpolation.linear)
			}

			state.world.addEntity(entity)
		}

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		useLeap = xmlData.getBoolean("UseLeap", false)
		alignToVector = xmlData.getBoolean("AlignToVector", true)
		spawnDuration = xmlData.getFloat("SpawnDuration", 0f)
	}
	override val classID: String = "FlightParticle"
	//endregion
}