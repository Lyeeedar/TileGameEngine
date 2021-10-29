package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Mesh.VertexDataType
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.GL30FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.NumberUtils
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.writeInstanceData
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics

class SpriteDrawerer(val renderer: SortedRenderer): Disposable
{
	internal var renderStatic = true

	internal val ambientLight = Colour()

	private class VertexBuffer
	{
		var offset = -1
		var count = -1
		lateinit var texture: Texture
		var blendSrc: Int = -2
		var blendDst: Int = -2

		init
		{

		}

		fun reset(blendSrc: Int, blendDst: Int, texture: Texture): VertexBuffer
		{
			this.blendSrc = blendSrc
			this.blendDst = blendDst
			this.texture = texture
			count = 0
			offset = 0

			return this
		}
	}

	private val lightMesh: Mesh
	private val shadowedLightMesh: Mesh
	private val shadowMesh: Mesh
	private val lightShader: ShaderProgram
	private val shadowedLightShader: ShaderProgram
	private val shadowShader: ShaderProgram
	private lateinit var lightFBO: GL30FrameBuffer
	private val lightFBOSize: Vector2 = Vector2()
	private val lightInstanceData: FloatArray
	private val shadowedLightInstanceData: FloatArray
	private val shadowedRegionUniformData: FloatArray
	private val shadowInstanceData: FloatArray
	private var lightBufferHash: Int = 0

	private val geometryMesh: Mesh
	private val staticGeometryMesh: Mesh
	private var currentBuffer: VertexBuffer? = null
	private val geometryInstanceData: FloatArray
	private var currentGeometryInstanceIndex = 0

	private val staticBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val queuedBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val spriteShader: ShaderProgram

	private val combinedMatrix: Matrix4 = Matrix4()

	private val bufferPool: Pool<VertexBuffer> = object : Pool<VertexBuffer>() {
		override fun newObject(): VertexBuffer
		{
			return VertexBuffer()
		}
	}

	private val precomputedVertexShader: ShaderProgram
	private val precomputedMesh: Mesh
	private val precomputedVertices: FloatArray
	private val precomputedIndices: ShortArray
	private var precomputedVertexCount = 0
	private var precomputedIndexCount = 0
	private val activePrecomputedVertexBuffer: VertexBuffer = VertexBuffer()

	init
	{
		val billboardVertices = floatArrayOf(-1f, +1f, -1f, -1f, +1f, -1f, +1f, +1f)
		val billboardIndices = shortArrayOf(0, 1, 2, 2, 3, 0)

		geometryMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		geometryMesh.setVertices(billboardVertices)
		geometryMesh.setIndices(billboardIndices)
		geometryMesh.enableInstancedRendering(false, maxInstances,
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_width_height"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords0"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords1"),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_blendAlpha_isLit_alphaRef_rotation"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_smoothLighting"))

		staticGeometryMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		staticGeometryMesh.setVertices(billboardVertices)
		staticGeometryMesh.setIndices(billboardIndices)
		staticGeometryMesh.enableInstancedRendering(true, maxInstances,
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_width_height"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords0"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords1"),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_blendAlpha_isLit_alphaRef_rotation"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_smoothLighting"))

		lightMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		lightMesh.setVertices(billboardVertices)
		lightMesh.setIndices(billboardIndices)
		lightMesh.enableInstancedRendering(false, 10000,
		                                   VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_range_brightness"),
		                                   VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE))

		shadowedLightMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		shadowedLightMesh.setVertices(billboardVertices)
		shadowedLightMesh.setIndices(billboardIndices)
		shadowedLightMesh.enableInstancedRendering(false, 100,
		                                           VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_range_brightness"),
		                                           VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                                           VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_region_offset_count"))

		shadowMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		shadowMesh.setVertices(billboardVertices)
		shadowMesh.setIndices(billboardIndices)
		shadowMesh.enableInstancedRendering(false, 100,
		                                    VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_width_height"),
		                                    VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords0"))

		val maxPrecomputedVertices = 2000
		precomputedMesh = Mesh(VertexDataType.VertexBufferObjectWithVAO, false, maxPrecomputedVertices, maxPrecomputedVertices * 2 * 3,
			VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
			VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
			VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
		precomputedVertices = FloatArray(maxPrecomputedVertices * 5)
		precomputedIndices = ShortArray(maxPrecomputedVertices * 2 * 3)

		geometryInstanceData = FloatArray(maxInstances * instanceDataSize)
		lightInstanceData = FloatArray(10000 * (4 + 1))
		shadowedLightInstanceData = FloatArray(100 * (4 + 1 + 2))
		shadowedRegionUniformData = FloatArray(128 * 4)
		shadowInstanceData = FloatArray(100 * (4 + 4))

		spriteShader = AssetManager.loadShader("Sprites/Shaders/geometry")
		lightShader = AssetManager.loadShader("Sprites/Shaders/light")
		shadowedLightShader = AssetManager.loadShader("Sprites/Shaders/shadowed_light")
		precomputedVertexShader = AssetManager.loadShader("Sprites/Shaders/precomputed_geometry", "Sprites/Shaders/geometry")
		shadowShader = AssetManager.loadShader("Sprites/Shaders/shadow")

		createFBO()
	}

	override fun dispose()
	{
		geometryMesh.dispose()
		staticGeometryMesh.dispose()
		lightMesh.dispose()
		lightFBO.dispose()
		shadowedLightMesh.dispose()
		spriteShader.dispose()
		lightShader.dispose()
		shadowedLightShader.dispose()
		precomputedVertexShader.dispose()
		precomputedMesh.dispose()
	}

	private fun updateFBO()
	{
		val width = Statics.stage.viewport.screenWidth / 2
		val height = Statics.stage.viewport.screenHeight / 2

		if (width != lightFBO.width || height != lightFBO.height)
		{
			lightFBO.dispose()
			createFBO()
		}
	}

	private fun createFBO()
	{
		val width = Statics.stage.viewport.screenWidth
		val height = Statics.stage.viewport.screenHeight

		lightFBO = GL30FrameBuffer(GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, width, height, false)
		lightFBO.colorBufferTexture?.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

		lightFBOSize.set(width.toFloat(), height.toFloat())
	}

	fun freeStaticBuffers()
	{
		for (buffer in staticBuffers)
		{
			bufferPool.free(buffer)
		}
		staticBuffers.clear()
	}

	private fun fillVertexBuffer(rs: RenderSprite)
	{
		if (currentGeometryInstanceIndex + instanceDataSize >= maxInstances * instanceDataSize)
		{
			System.err.println("Too many instances queued!")
			return
		}

		val blendSrc = rs.blend.src
		val blendDst = rs.blend.dst

		val sprite = rs.sprite
		var texture = rs.texture?.texture

		if (sprite != null)
		{
			texture = sprite.currentTexture.texture
		}

		if (texture == null)
		{
			throw RuntimeException("Rendersprite had a null texture and a null sprite!")
		}

		if (currentBuffer == null)
		{
			currentBuffer = bufferPool.obtain()
			currentBuffer!!.reset(blendSrc, blendDst, texture)
			currentBuffer!!.offset = currentGeometryInstanceIndex
		}

		var buffer = currentBuffer!!
		if (buffer.blendSrc != blendSrc || buffer.blendDst != blendDst || buffer.texture != texture)
		{
			queuedBuffers.add(currentBuffer)
			buffer = bufferPool.obtain()
			buffer.reset(blendSrc, blendDst, texture)
			buffer.offset = currentGeometryInstanceIndex

			currentBuffer = buffer
		}

		val offset = currentGeometryInstanceIndex
		buffer.count += instanceDataSize
		currentGeometryInstanceIndex += instanceDataSize

		val localx = rs.x
		val localy = rs.y
		val localw = rs.width * renderer.tileSize
		val localh = rs.height * renderer.tileSize

		val colour = rs.colour

		if (sprite != null)
		{
			val renderCol = sprite.getRenderColour()
			if (!renderCol.isWhite()) colour.mul(renderCol)

			sprite.render(geometryInstanceData, offset, colour, localx, localy, localw, localh, rs.scaleX, rs.scaleY, rs.rotation, rs.isLit)
		}
		else if (rs.texture != null)
		{
			writeInstanceData(geometryInstanceData, offset,
			       rs.texture!!, rs.nextTexture ?: rs.texture!!, colour,
			       localx, localy, 1f, 1f, localw * rs.scaleX, localh * rs.scaleY, rs.rotation, rs.flipX, rs.flipY,
			       0f, rs.blendAlpha, rs.alphaRef, rs.isLit, false)
		}
	}

	private fun updateLightBuffer()
	{
		updateFBO()

		var lightsHash = ambientLight.hashCode() xor lightFBO.hashCode()
		lightsHash = lightsHash xor NumberUtils.floatToIntBits(renderer.offsetx)
		lightsHash = lightsHash xor NumberUtils.floatToIntBits(renderer.offsety)
		lightsHash = lightsHash xor NumberUtils.floatToIntBits(renderer.tileSize)

		for (i in 0 until renderer.basicLights.size)
		{
			val light = renderer.basicLights[i]

			lightsHash = lightsHash xor light.pos.hashCode()
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.range)
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.brightness)
			lightsHash = lightsHash xor light.colour.hashCode()
		}

		for (i in 0 until renderer.shadowLights.size)
		{
			val light = renderer.shadowLights[i]

			lightsHash = lightsHash xor light.cache.castID
			lightsHash = lightsHash xor light.pos.hashCode()
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.range)
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.brightness)
			lightsHash = lightsHash xor light.colour.hashCode()
		}

		if (lightsHash != lightBufferHash)
		{
			lightBufferHash = lightsHash

			fillLightBuffer()
			renderLights()
		}

		for (i in 0 until renderer.shadows.size)
		{
			val shadow = renderer.shadows[i]

			for (ii in 0 until shadow.queuedPositions)
			{
				lightsHash = lightsHash xor shadow.positions[ii].hashCode()
			}

			lightsHash = lightsHash xor NumberUtils.floatToIntBits(shadow.scale)
		}
	}

	private fun fillLightBuffer()
	{
		var i = 0
		for (l in 0 until renderer.basicLights.size)
		{
			val light = renderer.basicLights[l]

			val x = light.pos.x * renderer.tileSize
			val y = light.pos.y * renderer.tileSize
			val range = light.range * renderer.tileSize
			val colourBrightness = light.colour.toScaledFloatBits()

			lightInstanceData[i++] = x
			lightInstanceData[i++] = y
			lightInstanceData[i++] = range * 0.9f
			lightInstanceData[i++] = colourBrightness.y * light.brightness
			lightInstanceData[i++] = colourBrightness.x
		}

		lightMesh.setInstanceData(lightInstanceData, 0, i)

		i = 0
		var r = 0
		for (l in 0 until renderer.shadowLights.size)
		{
			val light = renderer.shadowLights[l]

			val x = light.pos.x * renderer.tileSize
			val y = light.pos.y * renderer.tileSize
			val range = light.range * renderer.tileSize
			val colourBrightness = light.colour.toScaledFloatBits()
			val regions = light.cache.getOpaqueRegions()

			shadowedLightInstanceData[i++] = x
			shadowedLightInstanceData[i++] = y
			shadowedLightInstanceData[i++] = range * 0.9f
			shadowedLightInstanceData[i++] = colourBrightness.y * light.brightness
			shadowedLightInstanceData[i++] = colourBrightness.x
			shadowedLightInstanceData[i++] = r.toFloat() / 4f
			shadowedLightInstanceData[i++] = regions.size.toFloat()

			for (ri in 0 until regions.size)
			{
				val region = regions[ri]

				val x = region.x.toFloat() * renderer.tileSize
				val y = region.y.toFloat() * renderer.tileSize
				val w = region.width.toFloat() * renderer.tileSize
				val h = region.height.toFloat() * renderer.tileSize

				shadowedRegionUniformData[r++] = x
				shadowedRegionUniformData[r++] = y
				shadowedRegionUniformData[r++] = x+w
				shadowedRegionUniformData[r++] = y+h
			}
		}

		shadowedLightMesh.setInstanceData(shadowedLightInstanceData, 0, i)

		i = 0
		for (l in 0 until renderer.shadows.size)
		{
			val shadow = renderer.shadows[l]

			val width = shadow.scale * renderer.tileSize
			val height = shadow.scale * renderer.tileSize

			for (ii in 0 until shadow.queuedPositions)
			{
				val x = shadow.positions[ii].x * renderer.tileSize
				val y = shadow.positions[ii].y * renderer.tileSize

				shadowInstanceData[i++] = x
				shadowInstanceData[i++] = y
				shadowInstanceData[i++] = width
				shadowInstanceData[i++] = height
				shadowInstanceData[i++] = shadow.texture.u
				shadowInstanceData[i++] = shadow.texture.v
				shadowInstanceData[i++] = shadow.texture.u2
				shadowInstanceData[i++] = shadow.texture.v2
			}
		}

		shadowMesh.setInstanceData(shadowInstanceData, 0, i)
	}

	private fun renderLights()
	{
		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		lightFBO.begin()

		Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)

		Gdx.gl.glClearColor(ambientLight.r, ambientLight.g, ambientLight.b, 0f)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

		if (renderer.shadows.size > 0)
		{
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

			shadowShader.bind()
			shadowShader.setUniformMatrix("u_projTrans", combinedMatrix)
			shadowShader.setUniformf("u_offset", offsetx, offsety)
			shadowShader.setUniformi("u_texture", 0)
			shadowShader.setUniformf("u_colour", ambientLight.r, ambientLight.g, ambientLight.b)

			renderer.shadows[0].texture.texture.bind(0)

			shadowMesh.bind(shadowShader)

			shadowMesh.render(shadowShader, GL20.GL_TRIANGLES, 0, 6)

			shadowMesh.unbind(shadowShader)

			Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)
		}

		if (renderer.basicLights.size > 0)
		{
			lightShader.bind()
			lightShader.setUniformMatrix("u_projTrans", combinedMatrix)
			lightShader.setUniformf("u_offset", offsetx, offsety)

			lightMesh.bind(lightShader)

			lightMesh.render(lightShader, GL20.GL_TRIANGLES, 0, 6)

			lightMesh.unbind(lightShader)
		}

		if (renderer.shadowLights.size > 0)
		{
			shadowedLightShader.bind()
			shadowedLightShader.setUniformMatrix("u_projTrans", combinedMatrix)
			shadowedLightShader.setUniformf("u_offset", offsetx, offsety)
			shadowedLightShader.setUniform4fv("u_shadowRegions", shadowedRegionUniformData, 0, shadowedRegionUniformData.size)

			shadowedLightMesh.bind(shadowedLightShader)

			shadowedLightMesh.render(shadowedLightShader, GL20.GL_TRIANGLES, 0, 6)

			shadowedLightMesh.unbind(shadowedLightShader)
		}



		lightFBO.end(Statics.stage.viewport.screenX, Statics.stage.viewport.screenY, Statics.stage.viewport.screenWidth, Statics.stage.viewport.screenHeight)
	}

	private fun storeStatic()
	{
		queuedBuffers.add(currentBuffer!!)
		currentBuffer = null

		staticBuffers.addAll(queuedBuffers)
		queuedBuffers.clear()

		staticGeometryMesh.setInstanceData(geometryInstanceData, 0, currentGeometryInstanceIndex)
		currentGeometryInstanceIndex = 0

		if (staticBuffers.size > 1)
		{
			throw RuntimeException("Cannot queue more than 1 static buffer.")
		}
	}

	private fun fillPrecomputedVertexBuffer(rs: RenderSprite)
	{
		val texture = rs.texture!!.texture
		val blendSrc = rs.blend.src
		val blendDst = rs.blend.dst

		val currentBuffer = activePrecomputedVertexBuffer
		if (precomputedVertexCount == 0 ||
			currentBuffer.texture != texture ||
			currentBuffer.blendSrc != blendSrc ||
			currentBuffer.blendDst != blendDst)
		{
			renderPrecomputedVertices()
			currentBuffer.reset(blendSrc, blendDst, texture)
		}

		val vertices = rs.precomputedVertices!!
		val indices = rs.precomputedIndices!!

		val startIndex = precomputedVertexCount / 5
		for (i in 0 until indices.size)
		{
			precomputedIndices[precomputedIndexCount++] = (indices[i] + startIndex).toShort()
		}

		System.arraycopy(vertices, 0, precomputedVertices, precomputedVertexCount, vertices.size)
		precomputedVertexCount += vertices.size
	}

	private fun renderPrecomputedVertices()
	{
		if (precomputedVertexCount == 0) return
		val buffer = activePrecomputedVertexBuffer

		val shader = precomputedVertexShader

		shader.bind()

		shader.setUniformMatrix("u_projTrans", combinedMatrix)
		shader.setUniformi("u_texture", 0)
		shader.setUniformi("u_lightTexture", 1)
		shader.setUniformf("u_lightTextureSize", lightFBOSize)

		buffer.texture.bind(0)

		Gdx.gl.glBlendFunc(buffer.blendSrc, buffer.blendDst)

		// draw
		precomputedMesh.setVertices(precomputedVertices, 0, precomputedVertexCount)
		precomputedMesh.setIndices(precomputedIndices, 0, precomputedIndexCount)

		precomputedMesh.bind(shader)
		precomputedMesh.render(shader, GL20.GL_TRIANGLES, 0, precomputedIndexCount)
		precomputedMesh.unbind(shader)

		precomputedVertexCount = 0
		precomputedIndexCount = 0
	}

	private fun renderVertices()
	{
		if (currentBuffer == null && queuedBuffers.size == 0) return

		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		spriteShader.bind()

		spriteShader.setUniformMatrix("u_projTrans", combinedMatrix)
		spriteShader.setUniformf("u_offset", offsetx, offsety)
		spriteShader.setUniformi("u_texture", 0)
		spriteShader.setUniformi("u_lightTexture", 1)
		spriteShader.setUniformf("u_lightTextureSize", lightFBOSize)

		lightFBO.colorBufferTexture!!.bind(1)

		if (currentBuffer != null)
		{
			queuedBuffers.add(currentBuffer!!)
			currentBuffer = null
		}

		var lastBlendSrc = -1
		var lastBlendDst = -1
		var lastTexture: Texture? = null

		fun drawBuffer(buffer: VertexBuffer, mesh: Mesh)
		{
			if (buffer.texture != lastTexture)
			{
				buffer.texture.bind(0)
				lastTexture = buffer.texture
			}

			if (buffer.blendSrc != lastBlendSrc || buffer.blendDst != lastBlendDst)
			{
				Gdx.gl.glBlendFunc(buffer.blendSrc, buffer.blendDst)

				lastBlendSrc = buffer.blendSrc
				lastBlendDst = buffer.blendDst
			}

			mesh.render(spriteShader, GL20.GL_TRIANGLES, 0, 6)
		}

		if (queuedBuffers.size > 0)
		{
			for (buffer in queuedBuffers)
			{
				geometryMesh.setInstanceData(geometryInstanceData, buffer.offset, buffer.count)
				geometryMesh.bind(spriteShader)

				drawBuffer(buffer, geometryMesh)
				bufferPool.free(buffer)

				geometryMesh.unbind(spriteShader)
			}
			queuedBuffers.clear()
		}

		currentGeometryInstanceIndex = 0
	}

	private fun renderStatic()
	{
		if (staticBuffers.size == 0) return

		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		spriteShader.bind()

		spriteShader.setUniformMatrix("u_projTrans", combinedMatrix)
		spriteShader.setUniformf("u_offset", offsetx, offsety)
		spriteShader.setUniformi("u_texture", 0)
		spriteShader.setUniformi("u_lightTexture", 1)
		spriteShader.setUniformf("u_lightTextureSize", lightFBOSize)

		if (currentBuffer != null)
		{
			queuedBuffers.add(currentBuffer!!)
			currentBuffer = null
		}

		var lastBlendSrc = -1
		var lastBlendDst = -1
		var lastTexture: Texture? = null

		fun drawBuffer(buffer: VertexBuffer, mesh: Mesh)
		{
			if (buffer.texture != lastTexture)
			{
				buffer.texture.bind(0)
				lastTexture = buffer.texture
			}

			if (buffer.blendSrc != lastBlendSrc || buffer.blendDst != lastBlendDst)
			{
				Gdx.gl.glBlendFunc(buffer.blendSrc, buffer.blendDst)

				lastBlendSrc = buffer.blendSrc
				lastBlendDst = buffer.blendDst
			}

			mesh.render(spriteShader, GL20.GL_TRIANGLES, 0, 6)
		}

		if (staticBuffers.size > 0 && renderStatic)
		{
			staticGeometryMesh.bind(spriteShader)

			for (buffer in staticBuffers)
			{
				drawBuffer(buffer, staticGeometryMesh)
			}

			staticGeometryMesh.unbind(spriteShader)
		}
	}

	internal fun draw(batch: Batch?)
	{
		if (renderer.queuedSprites == 0 && staticBuffers.size == 0)
		{
			return
		}

		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glDepthMask(false)

		if (batch != null) combinedMatrix.set(batch.projectionMatrix).mul(batch.transformMatrix)

		if (!renderer.inStaticBegin)
		{
			updateLightBuffer()
		}
		lightFBO.colorBufferTexture!!.bind(1)

		// begin rendering
		if (!renderer.inStaticBegin)
		{
			renderStatic()
		}

		for (i in 0 until renderer.queuedSprites)
		{
			val rs = renderer.spriteArray[i]!!

			if (rs.precomputedVertices != null)
			{
				renderVertices()

				fillPrecomputedVertexBuffer(rs)
			}
			else
			{
				renderPrecomputedVertices()

				fillVertexBuffer(rs)

				if (!renderer.inStaticBegin && currentGeometryInstanceIndex + instanceDataSize >= maxInstances * instanceDataSize)
				{
					renderVertices()
				}
			}
		}

		if (renderer.inStaticBegin)
		{
			storeStatic()
		}
		else
		{
			renderVertices()
			renderPrecomputedVertices()
		}

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
	}

	companion object
	{
		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		public const val instanceDataSize = 4+4+4+1+1+1
		private const val maxInstances = 100000
	}
}