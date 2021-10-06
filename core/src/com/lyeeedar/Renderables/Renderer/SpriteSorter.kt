package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.*
import ktx.collections.set
import squidpony.squidmath.LightRNG

class SpriteSorter(val renderer: SortedRenderer)
{
	private var batchID: Int = random.nextInt()

	private val tempVec = Vector2()
	private val tempVec2 = Vector2()
	private val tempVec3 = Vector3()
	private val tempCol = Colour()
	private val bitflag = EnumBitflag<Direction>()

	private val MAX_INDEX = 5
	private val MAX_LAYER = renderer.layers
	private val NUM_BLENDS = BlendMode.values().size

	private val BLEND_BLOCK_SIZE = 1
	private val INDEX_BLOCK_SIZE = BLEND_BLOCK_SIZE * NUM_BLENDS
	private val LAYER_BLOCK_SIZE = INDEX_BLOCK_SIZE * MAX_INDEX
	private val X_BLOCK_SIZE = LAYER_BLOCK_SIZE * MAX_LAYER
	private val Y_BLOCK_SIZE = X_BLOCK_SIZE * renderer.width.toInt()

	internal fun updateBatchID()
	{
		batchID = random.nextInt()
	}

	internal fun sort()
	{
		// set tiling sprites
		for (i in 0 until renderer.queuedSprites)
		{
			val rs = renderer.spriteArray[i] ?: continue
			if (rs.tilingSprite != null)
			{
				bitflag.clear()
				for (dir in Direction.Values)
				{
					val hash = Point.getHashcode(rs.px, rs.py, dir)
					val keys = renderer.tilingMap[hash]

					if (keys?.contains(rs.tilingSprite!!.checkID) != true)
					{
						bitflag.setBit(dir)
					}
				}

				val sprite = rs.tilingSprite!!.getSprite(bitflag)
				rs.sprite = sprite

				if (sprite.light != null)
				{
					renderer.addLight(sprite.light!!, rs.px + 0.5f, rs.py + 0.5f)
				}
			}
		}

		renderer.spriteArray.sort(0, renderer.queuedSprites)
		renderer.basicLights.sort()
		renderer.shadowLights.sort()
	}

	private fun getComparisonVal(x: Float, y: Float, layer: Int, index: Int, blend: BlendMode) : Int
	{
		if (index > MAX_INDEX-1) throw RuntimeException("Index too high! $index >= $MAX_INDEX!")
		if (layer > MAX_LAYER-1) throw RuntimeException("Layer too high! $layer >= $MAX_LAYER!")

		val yBlock = y.floor() * Y_BLOCK_SIZE * -1
		val xBlock = x.floor() * X_BLOCK_SIZE * -1
		val lBlock = layer * LAYER_BLOCK_SIZE
		val iBlock = index * INDEX_BLOCK_SIZE
		val bBlock = blend.ordinal * BLEND_BLOCK_SIZE

		return yBlock + xBlock + lBlock + iBlock + bBlock
	}

	internal fun update(renderable: Renderable, deltaTime: Float? = null)
	{
		if (renderable.batchID != batchID) renderable.update(deltaTime ?: renderer.delta)
		renderable.batchID = batchID
	}

	private fun storeRenderSprite(renderSprite: RenderSprite)
	{
		if (renderer.queuedSprites == renderer.spriteArray.size-1)
		{
			renderer.spriteArray = renderer.spriteArray.copyOf(renderer.spriteArray.size * 2)
		}

		if (renderSprite.texture == null && renderSprite.sprite == null && renderSprite.tilingSprite == null)
		{
			throw RuntimeException("Queued a sprite with nothing to render!")
		}

		renderer.spriteArray[renderer.queuedSprites] = renderSprite

		renderer.queuedSprites++
	}

	internal fun queueParticle(effect: ParticleEffect, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, lit: Boolean)
	{
		val tileSize = renderer.tileSize

		var lx = ix
		var ly = iy

		if (effect.lockPosition)
		{

		}
		else
		{
			if (effect.facing.x != 0)
			{
				lx = ix + effect.size[1].toFloat() * 0.5f
				ly = iy + effect.size[0].toFloat() * 0.5f
			}
			else
			{
				if (effect.isCentered)
				{
					lx = ix + 0.5f
					ly = iy + 0.5f
				}
				else
				{
					lx = ix + effect.size[0].toFloat() * 0.5f
					ly = iy + effect.size[1].toFloat() * 0.5f
				}
			}

			effect.setPosition(lx, ly)
		}

		update(effect)

		if (!effect.visible) return
		if (effect.renderDelay > 0 && !effect.showBeforeRender)
		{
			return
		}

		val posOffset = effect.animation?.renderOffset(false)
		lx += (posOffset?.get(0) ?: 0f)
		ly += (posOffset?.get(1) ?: 0f)

		if (effect.faceInMoveDirection)
		{
			val angle = getRotation(effect.lastPos, tempVec.set(lx, ly))
			effect.rotation = angle
			effect.lastPos.set(lx, ly)
		}

		if (effect.light != null)
		{
			renderer.addLight(effect.light!!, lx, ly)
		}

		//val scale = effect.animation?.renderScale()?.get(0) ?: 1f
		val animCol = effect.animation?.renderColour() ?: Colour.WHITE

		for (emitter in effect.emitters)
		{
			for (particle in emitter.particles)
			{
				var px = 0f
				var py = 0f

				if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL)
				{
					tempVec.set(emitter.currentOffset)
					tempVec.scl(emitter.size)
					tempVec.rotate(emitter.rotation)

					px += (emitter.position.x + tempVec.x)
					py += (emitter.position.y + tempVec.y)
				}

				for (pdata in particle.particles)
				{
					val keyframe1 = pdata.keyframe1
					val keyframe2 = pdata.keyframe2
					val alpha = pdata.keyframeAlpha

					val tex1 = keyframe1.texture[pdata.texStream]
					val tex2 = keyframe2.texture[pdata.texStream]

					val col = tempCol.set(keyframe1.colour[pdata.colStream]).lerp(keyframe2.colour[pdata.colStream], alpha)
					col.a = keyframe1.alpha[pdata.alphaStream].lerp(keyframe2.alpha[pdata.alphaStream], alpha)

					val size = keyframe1.size[pdata.sizeStream].lerp(keyframe2.size[pdata.sizeStream], alpha, pdata.ranVal)

					var w = width
					var h = height
					if (particle.maintainAspectRatio)
					{
						w = min(width, height)
						h = w
					}

					var sizex = if (particle.sizeMode == Particle.SizeMode.YONLY) w else size * w
					var sizey = if (particle.sizeMode == Particle.SizeMode.XONLY) h else size * h

					if (particle.allowResize)
					{
						sizex *= emitter.size.x
						sizey *= emitter.size.y
					}

					val rotation = if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) pdata.rotation + emitter.rotation + emitter.emitterRotation else pdata.rotation

					col.mul(colour).mul(animCol).mul(effect.colour)

					tempVec.set(pdata.position)

					if (emitter.simulationSpace == Emitter.SimulationSpace.LOCAL) tempVec.scl(emitter.size).rotate(emitter.rotation + emitter.emitterRotation)

					var drawx = tempVec.x + px
					var drawy = tempVec.y + py

					when (particle.sizeOrigin)
					{
						Particle.SizeOrigin.CENTER -> { }
						Particle.SizeOrigin.BOTTOM -> {
							drawy += sizey*0.5f
						}
						Particle.SizeOrigin.TOP -> {
							drawy -= sizey*0.5f
						}
						Particle.SizeOrigin.LEFT -> {
							drawx += sizex*0.5f
						}
						Particle.SizeOrigin.RIGHT -> {
							drawx -= sizex*0.5f
						}
					}

					val localx = drawx * tileSize + renderer.offsetx
					val localy = drawy * tileSize + renderer.offsety
					val localw = sizex * tileSize
					val localh = sizey * tileSize

					if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) continue

					val comparisonVal = getComparisonVal(drawx-sizex*0.5f-1f, drawy-sizey*0.5f-1f, layer, index, particle.blend)

					val tex1Index = tex1.toInt()
					val texture1 = particle.textures[pdata.texStream][tex1Index].second

					val rs = RenderSprite.obtain().set(null, null, texture1, drawx * tileSize, drawy * tileSize, tempVec.x, tempVec.y, col, sizex, sizey, rotation, 1f, 1f, effect.flipX, effect.flipY, particle.blend, lit, comparisonVal)

					if (particle.blendKeyframes)
					{
						val tex2Index = min(particle.textures[pdata.texStream].size-1, tex1Index+1)
						val texture2 = particle.textures[pdata.texStream][tex2Index].second
						val blendAlpha = tex1.lerp(tex2, pdata.keyframeAlpha)

						rs.nextTexture = texture2
						rs.blendAlpha = blendAlpha
					}
					rs.alphaRef = keyframe1.alphaRef[pdata.alphaRefStream].lerp(keyframe2.alphaRef[pdata.alphaRefStream], alpha)

					storeRenderSprite(rs)
				}
			}
		}
	}

	private fun addToMap(tilingSprite: TilingSprite, ix: Float, iy: Float)
	{
		// Add to map
		val hash = Point.getHashcode(ix.toInt(), iy.toInt())
		var keys = renderer.tilingMap[hash]
		if (keys == null)
		{
			keys = renderer.setPool.obtain()
			keys.clear()

			renderer.tilingMap[hash] = keys
		}
		keys.add(tilingSprite.checkID)
	}

	internal fun queueSprite(tilingSprite: TilingSprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, lit: Boolean)
	{
		update(tilingSprite)

		if (!tilingSprite.visible) return
		if (tilingSprite.renderDelay > 0 && !tilingSprite.showBeforeRender)
		{
			return
		}

		val tileSize = renderer.tileSize

		var lx = ix
		var ly = iy

		var x = ix * tileSize
		var y = iy * tileSize

		if ( tilingSprite.animation != null )
		{
			val offset = tilingSprite.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}
		}

		addToMap(tilingSprite, ix, iy)

		if (tilingSprite.light != null)
		{
			renderer.addLight(tilingSprite.light!!, lx + 0.5f, ly + 0.5f)
		}

		// check if onscreen
		if (!renderer.alwaysOnscreen && !isSpriteOnscreen(tilingSprite, x, y, width, height)) return

		val comparisonVal = getComparisonVal(lx, ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, tilingSprite, null, x, y, ix, iy, colour, width, height, 0f, 1f, 1f, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	internal fun queueSprite(sprite: Sprite, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, scaleX: Float, scaleY: Float, lit: Boolean, sortX: Float?, sortY: Float?)
	{
		update(sprite)

		if (!sprite.visible) return
		if (sprite.renderDelay > 0 && !sprite.showBeforeRender)
		{
			return
		}

		val tileSize = renderer.tileSize

		var lx = ix
		var ly = iy

		var x = ix * tileSize
		var y = iy * tileSize

		var rotation = 0f

		var lScaleX = sprite.baseScale[0] * scaleX
		var lScaleY = sprite.baseScale[1] * scaleY

		if ( sprite.animation != null )
		{
			val offset = sprite.animation?.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}

			rotation = sprite.animation?.renderRotation() ?: 0f

			val scale = sprite.animation!!.renderScale()
			if (scale != null)
			{
				lScaleX *= scale[0]
				lScaleY *= scale[1]
			}
		}

		if (sprite.drawActualSize)
		{
			val widthRatio = width / 32f
			val regionWidth = sprite.currentTexture.regionWidth.toFloat()
			val trueWidth = regionWidth * widthRatio
			val widthOffset = (trueWidth - width) / 2

			lx -= widthOffset
		}

		lx = lx + 0.5f - (0.5f * lScaleX)
		ly = ly + 0.5f - (0.5f * lScaleY)

		if (sprite.faceInMoveDirection)
		{
			val angle = getRotation(sprite.lastPos, tempVec.set(x, y))
			sprite.rotation = angle
			sprite.lastPos.set(x, y)
		}

		if (sprite.light != null)
		{
			renderer.addLight(sprite.light!!, lx + 0.5f, ly + 0.5f)
		}

		// check if onscreen
		if (!renderer.alwaysOnscreen && !isSpriteOnscreen(sprite, x, y, width, height, scaleX, scaleY)) return

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(sprite, null, null, x, y, ix, iy, colour, width, height, rotation, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	internal fun queueTexture(texture: TextureRegion, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, scaleX: Float, scaleY: Float, lit: Boolean, sortX: Float?, sortY: Float?, rotation: Float?)
	{
		val tileSize = renderer.tileSize

		val lx = ix - width
		val ly = iy - height

		val x = ix * tileSize
		val y = iy * tileSize

		// check if onscreen

		val localx = x + renderer.offsetx
		val localy = y + renderer.offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(null, null, texture, x, y, ix, iy, colour, width, height, rotation ?: 0f, scaleX, scaleY, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

		storeRenderSprite(rs)
	}

	private fun isSpriteOnscreen(sprite: Sprite, x: Float, y: Float, width: Float, height: Float, scaleX: Float = 1f, scaleY: Float = 1f): Boolean
	{
		val tileSize = renderer.tileSize

		var localx = x + renderer.offsetx
		var localy = y + renderer.offsety
		var localw = width * tileSize * sprite.size[0]
		var localh = height * tileSize * sprite.size[1]

		var scaleX = sprite.baseScale[0] * scaleX
		var scaleY = sprite.baseScale[1] * scaleY

		if (sprite.animation != null)
		{
			val scale = sprite.animation!!.renderScale()
			if (scale != null)
			{
				scaleX *= scale[0]
				scaleY *= scale[1]
			}
		}

		if (sprite.drawActualSize)
		{
			val texture = sprite.textures.items[sprite.texIndex]

			val widthRatio = localw / 32f
			val heightRatio = localh / 32f

			val regionWidth = sprite.referenceSize ?: texture.regionWidth.toFloat()
			val regionHeight = sprite.referenceSize ?: texture.regionHeight.toFloat()

			val trueWidth = regionWidth * widthRatio
			val trueHeight = regionHeight * heightRatio

			val widthOffset = (trueWidth - localw) / 2f

			localx -= widthOffset
			localw = trueWidth
			localh = trueHeight
		}

		if (sprite.rotation != 0f && sprite.fixPosition)
		{
			val offset = Sprite.getPositionCorrectionOffsets(x, y, localw / 2.0f, localh / 2.0f, localw, localh, scaleX, scaleY, sprite.rotation, tempVec3)
			localx -= offset.x
			localy -= offset.y
		}

		if (scaleX != 1f)
		{
			val newW = localw * scaleX
			val diff = newW - localw

			localx -= diff * 0.5f
			localw = newW
		}
		if (scaleY != 1f)
		{
			val newH = localh * scaleY
			val diff = newH - localh

			localy -= diff * 0.5f
			localh = newH
		}

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return false

		return true
	}

	private fun isSpriteOnscreen(sprite: TilingSprite, x: Float, y: Float, width: Float, height: Float): Boolean
	{
		val tileSize = renderer.tileSize

		val localx = x + renderer.offsetx
		val localy = y + renderer.offsety
		val localw = width * tileSize
		val localh = height * tileSize

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return false

		return true
	}

	companion object
	{
		private val random = LightRNG()
	}
}