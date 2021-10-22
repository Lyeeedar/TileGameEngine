package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.NumberUtils
import com.esotericsoftware.spine.Skeleton
import com.esotericsoftware.spine.SkeletonRenderer.VertexEffect
import com.esotericsoftware.spine.Slot
import com.esotericsoftware.spine.attachments.ClippingAttachment
import com.esotericsoftware.spine.attachments.MeshAttachment
import com.esotericsoftware.spine.attachments.RegionAttachment
import com.esotericsoftware.spine.attachments.SkeletonAttachment
import com.esotericsoftware.spine.utils.SkeletonClipping
import com.lyeeedar.BlendMode
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Attachments.LightAttachment
import com.lyeeedar.Renderables.CurveRenderable
import com.lyeeedar.Renderables.Particle.Emitter
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Attachments.RenderableAttachment
import com.lyeeedar.Renderables.SkeletonRenderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Statics.Companion.spriteTargetResolution
import ktx.collections.set
import squidpony.squidmath.LightRNG

class SpriteSorter(val renderer: SortedRenderer)
{
	public var batchID: Int = random.nextInt()

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

	private val quadTriangles = shortArrayOf(0, 1, 2, 2, 3, 0)

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
				if (sprite.shadow != null)
				{
					renderer.addShadow(sprite.shadow!!, rs.px + 0.5f, rs.py + 0.5f)
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

		if (renderSprite.texture == null && renderSprite.sprite == null && renderSprite.tilingSprite == null && renderSprite.precomputedVertices == null)
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
		if (effect.shadow != null)
		{
			renderer.addShadow(effect.shadow!!, lx, ly)
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
		if (tilingSprite.shadow != null)
		{
			renderer.addShadow(tilingSprite.shadow!!, lx + 0.5f, ly + 0.5f)
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
			val widthRatio = width / spriteTargetResolution
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
		if (sprite.shadow != null)
		{
			renderer.addShadow(sprite.shadow!!, lx + 0.5f, ly + 0.5f)
		}

		// check if onscreen
		if (!renderer.alwaysOnscreen && !isSpriteOnscreen(sprite, x, y, width, height, lScaleX, lScaleY)) return

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		val rs = RenderSprite.obtain().set(sprite, null, null, x, y, ix, iy, colour, width, height, rotation, lScaleX, lScaleY, false, false, BlendMode.MULTIPLICATIVE, lit, comparisonVal)

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

	internal fun queueSkeleton(skeleton: SkeletonRenderable, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, scaleX: Float, scaleY: Float, lit: Boolean, sortX: Float?, sortY: Float?, rotation: Float?)
	{
		update(skeleton)
		val colour = tempCol.set(colour).mul(skeleton.colour)

		val tileSize = renderer.tileSize

		var x = ix * tileSize
		var y = iy * tileSize

		var lx = ix
		var ly = iy

		var lScaleX = scaleX
		var lScaleY = scaleY

		if (skeleton.animation != null)
		{
			val offset = skeleton.animation!!.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}

			val scale = skeleton.animation!!.renderScale()
			if (scale != null)
			{
				lScaleX *= scale[0]
				lScaleY *= scale[1]
			}

			val col = skeleton.animation?.renderColour()
			if (col != null)
			{
				colour.mul(col);
			}
		}

		lx = lx + 0.5f - (0.5f * lScaleX)
		ly = ly + 0.5f - (0.5f * lScaleY)

		val localx = x + renderer.offsetx
		val localy = y + renderer.offsety
		val localw = width * tileSize * skeleton.size[1]
		val localh = height * tileSize * skeleton.size[1]

		if (skeleton.light != null)
		{
			renderer.addLight(skeleton.light!!, lx + 0.5f, ly + 0.5f)
		}
		if (skeleton.shadow != null)
		{
			renderer.addShadow(skeleton.shadow!!, lx + 0.5f, ly + 0.5f)
		}

		if (localx + localw < 0 || localx > Statics.stage.width || localy + localh < 0 || localy > Statics.stage.height) return

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		if (skeleton.flipX) lScaleX *= -1f
		if (skeleton.flipY) lScaleY *= -1f

		skeleton.skeleton.setPosition(localx + localw * 0.5f, localy + localh * 0.3f)
		skeleton.skeleton.setScale(lScaleX * width * skeleton.size[0] * (tileSize / 40f), lScaleY * height * skeleton.size[1] * (tileSize / 40f))
		skeleton.skeleton.color = colour.color()
		skeleton.state.apply(skeleton.skeleton)
		skeleton.skeleton.updateWorldTransform()

		if (colour.a == 0f)
		{
			return
		}

		queueSkeleton(skeleton.skeleton, comparisonVal, layer, index)
	}

	private val vertices = com.badlogic.gdx.utils.FloatArray(32)
	private val clipper = SkeletonClipping()
	private val vertexEffect: VertexEffect? = null
	private val temp = Vector2()
	private val temp2 = Vector2()
	private val temp3 = Color()
	private val temp4 = Color()
	private val temp5 = Color()
	private val temp6 = Color()
	private fun queueSkeleton(skeleton: Skeleton, comparisonVal: Int, layer: Int, index: Int)
	{
		val tempPosition: Vector2 = this.temp
		val tempUV: Vector2 = this.temp2
		val tempLight1: Color = this.temp3
		val tempDark1: Color = this.temp4
		val tempLight2: Color = this.temp5
		val tempDark2: Color = this.temp6
		val vertexEffect: VertexEffect? = this.vertexEffect
		vertexEffect?.begin(skeleton)

		var blendMode: com.esotericsoftware.spine.BlendMode? = null
		var verticesLength = 0
		var vertices: FloatArray? = null
		var uvs: FloatArray? = null
		var triangles: ShortArray? = null
		var color: Color? = null
		val skeletonColor = skeleton.color
		val r = skeletonColor.r
		val g = skeletonColor.g
		val b = skeletonColor.b
		val a = skeletonColor.a
		val drawOrder = skeleton.drawOrder
		for (i in 0 until skeleton.drawOrder.size)
		{
			val slot = drawOrder[i] as Slot
			var texture: TextureRegion? = null
			val vertexSize = if (clipper.isClipping) 2 else 5
			val attachment = slot.attachment
			if (attachment is RegionAttachment)
			{
				val region = attachment
				verticesLength = vertexSize shl 2
				vertices = this.vertices.items
				region.computeWorldVertices(slot.bone, vertices, 0, vertexSize)
				triangles = quadTriangles
				texture = region.region
				uvs = region.uVs
				color = region.color
			}
			else if (attachment is MeshAttachment)
			{
				val mesh = attachment
				val count = mesh.worldVerticesLength
				verticesLength = (count shr 1) * vertexSize
				vertices = this.vertices.setSize(verticesLength)
				mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize)
				triangles = mesh.triangles
				texture = mesh.region
				uvs = mesh.uVs
				color = mesh.color
			}
			else if (attachment is ClippingAttachment)
			{
				clipper.clipStart(slot, attachment)
				continue
			}
			else if (attachment is SkeletonAttachment)
			{
				val attachmentSkeleton = attachment.skeleton
				attachmentSkeleton.updateWorldTransform(slot.bone)
				queueSkeleton(attachmentSkeleton, comparisonVal, layer, index)
			}
			else if (attachment is RenderableAttachment)
			{
				val renderable = attachment.renderable

				var wx = slot.bone.worldX
				var wy = slot.bone.worldY

				wx = (wx - renderer.offsetx) / renderer.tileSize - 0.5f
				wy = (wy - renderer.offsety) / renderer.tileSize - 0.5f
				renderer.queue(renderable, wx, wy, layer, index, Colour(slot.color))

				if (renderable is ParticleEffect && renderable.completed)
				{
					slot.attachment = null
				}
				else if (renderable is Sprite && renderable.completed)
				{
					slot.attachment = null
				}
			}
			else if (attachment is LightAttachment)
			{
				var wx = slot.bone.worldX
				var wy = slot.bone.worldY

				wx = (wx - renderer.offsetx) / renderer.tileSize
				wy = (wy - renderer.offsety) / renderer.tileSize
				renderer.addLight(attachment.light, wx, wy)
			}

			if (texture != null)
			{
				val slotColor = slot.color
				val alpha = a * slotColor.a * color!!.a * 255
				val multiplier: Float = 255f
				val slotBlendMode = slot.data.blendMode
				if (slotBlendMode != blendMode)
				{
					blendMode = slotBlendMode
				}
				val c = NumberUtils.intToFloatColor(
					alpha.toInt() shl 24 //
						or ((b * slotColor.b * color.b * multiplier).toInt() shl 16 //
						) or ((g * slotColor.g * color.g * multiplier).toInt() shl 8 //
						) or (r * slotColor.r * color.r * multiplier).toInt()
				                                   )
				if (clipper.isClipping)
				{
					clipper.clipTriangles(vertices, verticesLength, triangles, triangles!!.size, uvs, c, 0f, false)
					val clippedVertices = clipper.clippedVertices
					val clippedTriangles = clipper.clippedTriangles
					if (vertexEffect != null)
					{
						applyVertexEffect(clippedVertices.items, clippedVertices.size, 5, c, 0f)
					}

					val rs = RenderSprite.obtain().set(clippedVertices!!.items, clippedTriangles!!.items, texture, BlendMode.MULTIPLICATIVE, comparisonVal)
					storeRenderSprite(rs)
				}
				else
				{
					if (vertexEffect != null)
					{
						tempLight1.set(NumberUtils.floatToIntColor(c))
						tempDark1.set(0)
						var v = 0
						var u = 0
						while (v < verticesLength)
						{
							tempPosition.x = vertices!![v]
							tempPosition.y = vertices[v + 1]
							tempLight2.set(tempLight1)
							tempDark2.set(tempDark1)
							tempUV.x = uvs!![u]
							tempUV.y = uvs[u + 1]
							vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
							vertices[v] = tempPosition.x
							vertices[v + 1] = tempPosition.y
							vertices[v + 2] = tempLight2.toFloatBits()
							vertices[v + 3] = tempUV.x
							vertices[v + 4] = tempUV.y
							v += 5
							u += 2
						}
					}
					else
					{
						var v = 2
						var u = 0
						while (v < verticesLength)
						{
							vertices!![v] = c
							vertices[v + 1] = uvs!![u]
							vertices[v + 2] = uvs[u + 1]
							v += 5
							u += 2
						}
					}
					val rs = RenderSprite.obtain().set(vertices!!, triangles!!, texture, BlendMode.MULTIPLICATIVE, comparisonVal)
					storeRenderSprite(rs)
				}
			}
			clipper.clipEnd(slot)
		}
		clipper.clipEnd()
		vertexEffect?.end()
	}

	private fun applyVertexEffect(vertices: FloatArray, verticesLength: Int, stride: Int, light: Float, dark: Float)
	{
		val tempPosition: Vector2 = this.temp
		val tempUV: Vector2 = this.temp2
		val tempLight1: Color = this.temp3
		val tempDark1: Color = this.temp4
		val tempLight2: Color = this.temp5
		val tempDark2: Color = this.temp6
		val vertexEffect: VertexEffect = this.vertexEffect!!
		tempLight1.set(NumberUtils.floatToIntColor(light))
		tempDark1.set(NumberUtils.floatToIntColor(dark))
		if (stride == 5)
		{
			var v = 0
			while (v < verticesLength)
			{
				tempPosition.x = vertices[v]
				tempPosition.y = vertices[v + 1]
				tempUV.x = vertices[v + 3]
				tempUV.y = vertices[v + 4]
				tempLight2.set(tempLight1)
				tempDark2.set(tempDark1)
				vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
				vertices[v] = tempPosition.x
				vertices[v + 1] = tempPosition.y
				vertices[v + 2] = tempLight2.toFloatBits()
				vertices[v + 3] = tempUV.x
				vertices[v + 4] = tempUV.y
				v += stride
			}
		}
		else
		{
			var v = 0
			while (v < verticesLength)
			{
				tempPosition.x = vertices[v]
				tempPosition.y = vertices[v + 1]
				tempUV.x = vertices[v + 4]
				tempUV.y = vertices[v + 5]
				tempLight2.set(tempLight1)
				tempDark2.set(tempDark1)
				vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2)
				vertices[v] = tempPosition.x
				vertices[v + 1] = tempPosition.y
				vertices[v + 2] = tempLight2.toFloatBits()
				vertices[v + 3] = tempDark2.toFloatBits()
				vertices[v + 4] = tempUV.x
				vertices[v + 5] = tempUV.y
				v += stride
			}
		}
	}

	internal fun queueCurve(curve: CurveRenderable, ix: Float, iy: Float, layer: Int, index: Int, colour: Colour, width: Float, height: Float, scaleX: Float, scaleY: Float, lit: Boolean, sortX: Float?, sortY: Float?)
	{
		update(curve)

		val colour = tempCol.set(colour).mul(curve.colour)

		val tileSize = renderer.tileSize

		var x = ix * tileSize
		var y = iy * tileSize

		var lx = ix - width
		var ly = iy - height

		val curveMiddle = curve.path.valueAt(temp, 0.5f)
		lx = curveMiddle.x
		ly = curveMiddle.y

		var lScaleX = scaleX
		var lScaleY = scaleY

		if (curve.animation != null)
		{
			val offset = curve.animation!!.renderOffset(false)

			if (offset != null)
			{
				x += offset[0] * tileSize
				y += offset[1] * tileSize

				lx += offset[0]
				ly += offset[1]
			}

			val scale = curve.animation!!.renderScale()
			if (scale != null)
			{
				lScaleX *= scale[0]
				lScaleY *= scale[1]
			}

			val col = curve.animation?.renderColour()
			if (col != null)
			{
				colour.mul(col);
			}
		}

		lx = lx + 0.5f - (0.5f * lScaleX)
		ly = ly + 0.5f - (0.5f * lScaleY)

		if (curve.light != null)
		{
			renderer.addLight(curve.light!!, lx + 0.5f, ly + 0.5f)
		}
		if (curve.shadow != null)
		{
			renderer.addShadow(curve.shadow!!, lx + 0.5f, ly + 0.5f)
		}

		val comparisonVal = getComparisonVal(sortX ?: lx, sortY ?: ly, layer, index, BlendMode.MULTIPLICATIVE)

		curve.computeVertices(renderer.offsetx, renderer.offsety, tileSize, colour)

		val rs = RenderSprite.obtain().set(curve.vertices, curve.indices, curve.texture, BlendMode.MULTIPLICATIVE, comparisonVal)
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

			val widthRatio = localw / spriteTargetResolution
			val heightRatio = localh / spriteTargetResolution

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