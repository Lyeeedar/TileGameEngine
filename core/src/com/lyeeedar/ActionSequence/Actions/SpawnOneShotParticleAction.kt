package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.position
import com.lyeeedar.Components.renderable
import com.lyeeedar.Components.transient
import com.lyeeedar.Components.transientParticleArchetype
import com.lyeeedar.Renderables.Attachments.RenderableAttachment
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.SkeletonRenderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Renderable", name = "OneShotFX")
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

	@DataValue(visibleIf = "AttachmentSlot == null")
	var spawnSingleParticle: Boolean = false

	@DataValue(visibleIf = "AttachmentSlot == null && SpawnSingleParticle == false")
	var spawnBehaviour: SpawnBehaviour = SpawnBehaviour.IMMEDIATE

	@DataValue(visibleIf = "SpawnBehaviour != Immediate")
	var spawnDuration: Float = 0f
	var makeParticleNonBlocking: Boolean = false

	@DataValue(visibleIf = "SpawnSingleParticle == false")
	var attachmentSlot: String? = null

	override fun enter(state: ActionSequenceState)
	{
		val sourceTile = state.sourcePoint

		if (attachmentSlot?.isNotEmpty() == true)
		{
			val renderable = state.source.get()?.renderable()?.renderable as? SkeletonRenderable

			if (renderable != null)
			{
				var slot = renderable.skeleton.findSlot(attachmentSlot)
				if (slot == null)
				{
					for (child in renderable.attachedSkeletons)
					{
						slot = child.skeleton.findSlot(attachmentSlot)
						if (slot != null) break
					}
				}

				if (slot != null)
				{
					val r = particle.getParticleEffect()
					if (alignToVector)
					{
						r.rotation = state.facing.angle
					}

					val attachment = RenderableAttachment(r, "OneShotFx")
					slot.attachment = attachment
				}
			}

			return
		}

		if (state.targets.size == 0) return

		val min = state.targets.minByOrNull(Point::hashCode)!!
		val max = state.targets.maxByOrNull(Point::hashCode)!!

		if (spawnSingleParticle)
		{
			val entity = transientParticleArchetype.build()

			if (makeParticleNonBlocking)
			{
				entity.transient()!!.blocksTurns = false
			}

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
		}
		else
		{
			val furthest = state.targets.maxByOrNull{ it.dist(sourceTile) }!!

			for (point in state.targets)
			{
				val tile = state.world.grid.tryGet(point, null) ?: continue

				val entity = transientParticleArchetype.build()

				if (makeParticleNonBlocking)
				{
					entity.transient()!!.blocksTurns = false
				}

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
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		particle = AssetManager.loadParticleEffect(xmlData.getChildByName("Particle")!!)
		alignToVector = xmlData.getBoolean("AlignToVector", true)
		spawnSingleParticle = xmlData.getBoolean("SpawnSingleParticle", false)
		spawnBehaviour = SpawnBehaviour.valueOf(xmlData.get("SpawnBehaviour", SpawnBehaviour.IMMEDIATE.toString())!!.uppercase(Locale.ENGLISH))
		spawnDuration = xmlData.getFloat("SpawnDuration", 0f)
		makeParticleNonBlocking = xmlData.getBoolean("MakeParticleNonBlocking", false)
		attachmentSlot = xmlData.get("AttachmentSlot", null)
	}
	override val classID: String = "SpawnOneShotParticle"
	//endregion
}