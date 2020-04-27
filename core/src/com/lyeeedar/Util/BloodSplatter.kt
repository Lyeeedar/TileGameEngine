package com.lyeeedar.Util

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Systems.World
import ktx.math.times

class BloodSplatter
{
	companion object
	{
		val bloodArchetype = EntityArchetypeBuilder()
			.add(ComponentType.Renderable)
			.add(ComponentType.Position)

		val splatters = AssetManager.loadSprite("Oryx/Custom/terrain/bloodsplatter")

		fun splatter(attackSource: Point, bloodSource: Point, emitDist: Float, world: World<*>)
		{
			var vector = bloodSource.toVec()
			vector.sub(attackSource.x.toFloat(), attackSource.y.toFloat())
			vector.nor()

			val angle = 45f
			vector = vector.rotate(Random.random(Random.sharedRandom, -angle, angle))

			val dist = Random.randomWeighted(Random.sharedRandom) * emitDist
			vector = vector.scl(dist)

			val intVec = Point(vector.x.toInt(), vector.y.toInt())
			val remainderOffset = Vector2(vector.x - intVec.x, vector.y - intVec.y)
			var bloodDest = bloodSource + intVec

			val line = BresenhamLine.line(bloodSource, bloodDest, world.grid) ?: return
			bloodDest = line.last()
			val actualVector = bloodDest.copy().minus(bloodSource).toVec().add(remainderOffset)

			val chosen = splatters.textures.random()

			val sprite = Sprite(chosen)
			sprite.baseScale[0] = 0.3f + Random.random(Random.sharedRandom, 0.3f)
			sprite.baseScale[1] = sprite.baseScale[0]
			sprite.rotation = Random.random(Random.sharedRandom, 180f)
			sprite.colour = Colour(1f, 0.6f + Random.random(Random.sharedRandom, 0.2f), 0.6f + Random.random(Random.sharedRandom, 0.2f), 0.4f + Random.random(Random.sharedRandom, 0.2f))

			val entity = bloodArchetype.build()

			entity.renderable()?.set(sprite)

			val pos = entity.position()!!
			pos.position = bloodDest
			pos.offset.set(remainderOffset)

			val actualDist = actualVector.len()
			val animDur = 0.1f + 0.15f * actualDist
			sprite.animation = LeapAnimation.obtain().set(animDur, actualVector * -1f, Vector2(), 0.1f + 0.1f * actualDist)
			sprite.animation!!.isBlocking = false

			world.addEntity(entity)
		}
	}
}