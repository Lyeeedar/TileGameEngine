package com.lyeeedar.Util

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Systems.World
import ktx.math.times

class BloodSplatter
{
	companion object
	{
		val bloodArchetype = EntityArchetypeBuilder()
			.add(ComponentType.Renderable)
			.add(ComponentType.Position)
			.add(ComponentType.Blood)

		val splatters = AssetManager.loadSprite("Oryx/Custom/terrain/bloodsplatter")

		fun splatter(attackSource: Point, bloodSource: Point, emitDist: Float, world: World<*>, colour: Colour = Colour.RED)
		{
			var vector = bloodSource.toVec()
			vector.sub(attackSource.xFloat, attackSource.yFloat)
			vector.nor()

			val angle = Random.random(Random.sharedRandom, -45f, 45f)
			vector = vector.rotate(angle)

			val dist = Random.randomWeighted(Random.sharedRandom) * emitDist
			vector = vector.scl(dist)

			val chosen = splatters.textures.random()

			val sprite = Sprite(chosen)
			sprite.baseScale[0] = 0.3f + Random.random(Random.sharedRandom, 0.3f)
			sprite.baseScale[1] = sprite.baseScale[0]
			sprite.rotation = Random.random(Random.sharedRandom, 180f)
			sprite.colour = colour.copy().a(0.4f + Random.random(Random.sharedRandom, 0.2f))

			val entity = bloodArchetype.build()

			entity.blood()!!.originalA = sprite.colour.a
			entity.blood()!!.originalScale = sprite.baseScale[0]

			entity.renderable()?.set(sprite)

			val pos = entity.position()!!
			pos.slot = SpaceSlot.FLOORDETAIL
			pos.position = bloodSource
			pos.offset.set(vector)

			val actualDist = vector.len()
			val animDur = 0.05f + 0.1f * actualDist
			sprite.animation = LeapAnimation.obtain().set(animDur, vector * -1f, Vector2(), 0.1f + 0.1f * actualDist)
			sprite.animation!!.isBlocking = false

			world.addEntity(entity)
		}
	}
}