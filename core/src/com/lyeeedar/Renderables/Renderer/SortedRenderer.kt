package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.Statics

// ----------------------------------------------------------------------
class SortedRenderer(var tileSize: Float, val width: Float, val height: Float, val layers: Int, val alwaysOnscreen: Boolean) : Disposable
{
	internal val basicLights = com.badlogic.gdx.utils.Array<Light>()
	internal val shadowLights = com.badlogic.gdx.utils.Array<Light>()

	private val startingArraySize = 128
	internal var spriteArray = Array<RenderSprite?>(startingArraySize) { null }
	internal var queuedSprites = 0

	internal var delta: Float = 0f

	internal var inBegin = false
	internal var inStaticBegin = false
	internal var offsetx: Float = 0f
	internal var offsety: Float = 0f

	private var screenShakeRadius: Float = 0f
	private var screenShakeAccumulator: Float = 0f
	private var screenShakeSpeed: Float = 0f
	private var screenShakeAngle: Float = 0f
	private var screenShakeLocked: Boolean = false

	internal val tilingMap: IntMap<ObjectSet<Long>> = IntMap()
	internal val setPool: Pool<ObjectSet<Long>> = object : Pool<ObjectSet<Long>>() {
		override fun newObject(): ObjectSet<Long>
		{
			return ObjectSet()
		}
	}

	private val sorter: SpriteSorter = SpriteSorter(this)
	private val drawerer: SpriteDrawerer = SpriteDrawerer(this)

	//region screenshake

	fun setScreenShake(amount: Float, speed: Float)
	{
		screenShakeRadius = amount
		screenShakeSpeed = speed
	}

	fun lockScreenShake()
	{
		screenShakeLocked = true
	}

	fun unlockScreenShake()
	{
		screenShakeLocked = false
	}

	//endregion

	//region begin/end

	fun begin(deltaTime: Float, offsetx: Float, offsety: Float, ambientLight: Colour)
	{
		if (inBegin) throw Exception("Begin called again before flush!")

		drawerer.ambientLight.set(ambientLight)
		this.offsetx = offsetx
		this.offsety = offsety
		delta = deltaTime
		inBegin = true
	}

	fun beginStatic(offsetx: Float, offsety: Float, ambientLight: Colour)
	{
		if (inBegin) throw Exception("BeginStatic called within begin!")
		if (inStaticBegin) throw Exception("BeginStatic called BeginStatic!")

		drawerer.freeStaticBuffers()

		drawerer.ambientLight.set(ambientLight)
		this.offsetx = offsetx
		this.offsety = offsety
		delta = 0f
		inStaticBegin = true
	}

	fun end(batch: Batch)
	{
		if (!inBegin) throw Exception("End called before begin!")

		flush(batch)

		inBegin = false
	}

	fun endStatic()
	{
		if (!inStaticBegin) throw Exception("EndStatic called before beginstatic!")

		flush()

		inStaticBegin = false
	}

	private fun flush(batch: Batch? = null)
	{
		sorter.sort()

		// do screen shake
		if ( screenShakeRadius > 2 )
		{
			screenShakeAccumulator += delta

			while ( screenShakeAccumulator >= screenShakeSpeed )
			{
				screenShakeAccumulator -= screenShakeSpeed
				screenShakeAngle += (150 + Random.random(Random.sharedRandom) * 60)

				if (!screenShakeLocked)
				{
					screenShakeRadius *= 0.9f
				}
			}

			offsetx += Math.sin( screenShakeAngle.toDouble() ).toFloat() * screenShakeRadius
			offsety += Math.cos( screenShakeAngle.toDouble() ).toFloat() * screenShakeRadius
		}

		for (light in basicLights)
		{
			light.update(delta)
		}

		for (light in shadowLights)
		{
			light.update(delta)
		}

		drawerer.draw(batch)

		cleanup(!inStaticBegin)
	}

	private fun cleanup(updateBatchID: Boolean)
	{
		// clean up
		for (i in 0 until queuedSprites)
		{
			val rs = spriteArray[i]!!
			rs.free()
		}

		if (updateBatchID) sorter.updateBatchID()

		Particle.generateBrownianVectors()

		for (entry in tilingMap)
		{
			setPool.free(entry.value)
		}
		tilingMap.clear()

		basicLights.clear()
		shadowLights.clear()

		if (queuedSprites < spriteArray.size / 4)
		{
			spriteArray = kotlin.Array(spriteArray.size / 4, { null })
		}

		queuedSprites = 0
	}

	//endregion

	//region queue methods

	internal fun addLight(light: Light, ix: Float, iy: Float)
	{
		light.pos.set(ix, iy)

		if (!isLightOnscreen(light)) return

		if (Statics.lightCollisionGrid != null && light.hasShadows)
		{
			shadowLights.add(light)
		}
		else
		{
			basicLights.add(light)
		}
	}

	private fun isLightOnscreen(light: Light): Boolean
	{
		val tileSize = tileSize

		val x = light.pos.x * tileSize + offsetx
		val y = light.pos.y * tileSize + offsety
		val range = light.range * tileSize

		if (x + range <= 0 || x - range >= Statics.stage.width || y + range <= 0 || y - range >= Statics.stage.height) return false

		return true
	}

	fun queue(renderable: Renderable, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f)
	{
		if (renderable is Sprite) queueSprite(renderable, ix, iy, layer, index, colour, width, height)
		else if (renderable is TilingSprite) queueSprite(renderable, ix, iy, layer, index, colour, width, height)
		else if (renderable is ParticleEffect) queueParticle(renderable, ix, iy, layer, index, colour, width, height)
		else throw Exception("Unknown renderable type! " + renderable.javaClass)
	}

	fun queueParticle(effect: ParticleEffect, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")
		sorter.queueParticle(effect, ix, iy, layer, index, colour, width, height, lit)
	}

	fun queueSpriteWrapper(spriteWrapper: SpriteWrapper, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, lit: Boolean = true, sortX: Float? = null, sortY: Float? = null)
	{
		val sprite = spriteWrapper.getChosenSprite(ix.toInt(), iy.toInt())
		if (sprite != null)
		{
			queueSprite(sprite, ix, iy, layer, index, colour, width, height, scaleX, scaleY, lit, sortX, sortY)
		}

		val tilingSprite = spriteWrapper.getChosenTilingSprite(ix.toInt(), iy.toInt())
		if (tilingSprite != null)
		{
			queueSprite(tilingSprite, ix, iy, layer, index, colour, width, height, lit)
		}
	}

	fun queueSprite(tilingSprite: TilingSprite, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, lit: Boolean = true)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")
		sorter.queueSprite(tilingSprite, ix, iy, layer, index, colour, width, height, lit)
	}

	fun queueSprite(sprite: Sprite, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, lit: Boolean = true, sortX: Float? = null, sortY: Float? = null)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")
		sorter.queueSprite(sprite, ix, iy, layer, index, colour, width, height, scaleX, scaleY, lit, sortX, sortY)
	}

	fun queueTexture(texture: TextureRegion, ix: Float, iy: Float, layer: Int = 0, index: Int = 0, colour: Colour = Colour.WHITE, width: Float = 1f, height: Float = 1f, scaleX: Float = 1f, scaleY: Float = 1f, lit: Boolean = true, sortX: Float? = null, sortY: Float? = null, rotation: Float? = null)
	{
		if (!inBegin && !inStaticBegin) throw Exception("Queue called before begin!")
		sorter.queueTexture(texture, ix, iy, layer, index, colour, width, height, scaleX, scaleY, lit, sortX, sortY, rotation)
	}

	//endregion

	fun update(renderable: Renderable, deltaTime: Float)
	{
		sorter.update(renderable, deltaTime)
	}

	override fun dispose()
	{
		drawerer.dispose()
	}
}