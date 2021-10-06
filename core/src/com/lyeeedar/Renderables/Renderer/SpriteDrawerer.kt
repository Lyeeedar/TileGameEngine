package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.GL30FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.NumberUtils
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Renderables.writeInstanceData
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
		var blendSrc: Int = -1
		var blendDst: Int = -1

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
	private val shadowLightMesh: Mesh
	private val lightShader: ShaderProgram
	private val shadowLightShader: ShaderProgram
	private lateinit var lightFBO: GL30FrameBuffer
	private val lightFBOSize: Vector2 = Vector2()
	private val lightInstanceData: FloatArray
	private val shadowLightInstanceData: FloatArray
	private val shadowRegionUniformData: FloatArray
	private var lightBufferHash: Int = 0

	private val geometryMesh: Mesh
	private val staticGeometryMesh: Mesh
	private var currentBuffer: VertexBuffer? = null
	private val geometryInstanceData: FloatArray
	private var currentGeometryInstanceIndex = 0

	private val staticBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val queuedBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val shader: ShaderProgram

	private val combinedMatrix: Matrix4 = Matrix4()

	private val bufferPool: Pool<VertexBuffer> = object : Pool<VertexBuffer>() {
		override fun newObject(): VertexBuffer
		{
			return VertexBuffer()
		}
	}

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

		shadowLightMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		shadowLightMesh.setVertices(billboardVertices)
		shadowLightMesh.setIndices(billboardIndices)
		shadowLightMesh.enableInstancedRendering(false, 100,
		                                   VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_range_brightness"),
		                                   VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                                   VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_region_offset_count"))

		geometryInstanceData = FloatArray(maxInstances * instanceDataSize)
		lightInstanceData = FloatArray(10000 * (4 + 1))
		shadowLightInstanceData = FloatArray(100 * (4 + 1 + 2))
		shadowRegionUniformData = FloatArray(128 * 4)

		shader = createShader()
		lightShader = createLightShader()
		shadowLightShader = createShadowLightShader()

		createFBO()
	}

	override fun dispose()
	{
		geometryMesh.dispose()
		staticGeometryMesh.dispose()
		lightMesh.dispose()
		lightFBO.dispose()
		shader.dispose()
		lightShader.dispose()
		shadowLightShader.dispose()
	}

	fun updateFBO()
	{
		val width = Statics.stage.viewport.screenWidth / 2
		val height = Statics.stage.viewport.screenHeight / 2

		if (width != lightFBO.width || height != lightFBO.height)
		{
			lightFBO.dispose()
			createFBO()
		}
	}

	fun createFBO()
	{
		val width = Statics.stage.viewport.screenWidth / 2
		val height = Statics.stage.viewport.screenHeight / 2

		lightFBO = GL30FrameBuffer(GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, width, height, false)
		lightFBO.colorBufferTexture?.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

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
		}

		for (i in 0 until renderer.shadowLights.size)
		{
			val light = renderer.shadowLights[i]

			lightsHash = lightsHash xor light.cache.castID
			lightsHash = lightsHash xor light.pos.hashCode()
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.range)
			lightsHash = lightsHash xor NumberUtils.floatToIntBits(light.brightness)
		}

		if (lightsHash != lightBufferHash)
		{
			lightBufferHash = lightsHash

			fillLightBuffer()
			renderLights()
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

			shadowLightInstanceData[i++] = x
			shadowLightInstanceData[i++] = y
			shadowLightInstanceData[i++] = range * 0.9f
			shadowLightInstanceData[i++] = colourBrightness.y * light.brightness
			shadowLightInstanceData[i++] = colourBrightness.x
			shadowLightInstanceData[i++] = r.toFloat() / 4f
			shadowLightInstanceData[i++] = regions.size.toFloat()

			for (ri in 0 until regions.size)
			{
				val region = regions[ri]

				val x = region.x.toFloat() * renderer.tileSize
				val y = region.y.toFloat() * renderer.tileSize
				val w = region.width.toFloat() * renderer.tileSize
				val h = region.height.toFloat() * renderer.tileSize

				shadowRegionUniformData[r++] = x
				shadowRegionUniformData[r++] = y
				shadowRegionUniformData[r++] = x+w
				shadowRegionUniformData[r++] = y+h
			}
		}

		shadowLightMesh.setInstanceData(shadowLightInstanceData, 0, i)
	}

	private fun renderLights()
	{
		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		lightFBO.begin()

		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)
		Gdx.gl.glDepthMask(false)

		Gdx.gl.glClearColor(ambientLight.r, ambientLight.g, ambientLight.b, 0f)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

		if (renderer.basicLights.size > 0)
		{
			lightShader.begin()
			lightShader.setUniformMatrix("u_projTrans", combinedMatrix)
			lightShader.setUniformf("u_offset", offsetx, offsety)

			lightMesh.bind(lightShader)

			lightMesh.render(lightShader, GL20.GL_TRIANGLES, 0, 6)

			lightMesh.unbind(lightShader)
		}

		if (renderer.shadowLights.size > 0)
		{
			shadowLightShader.begin()
			shadowLightShader.setUniformMatrix("u_projTrans", combinedMatrix)
			shadowLightShader.setUniformf("u_offset", offsetx, offsety)
			shadowLightShader.setUniform4fv("u_shadowRegions", shadowRegionUniformData, 0, shadowRegionUniformData.size)

			shadowLightMesh.bind(shadowLightShader)

			shadowLightMesh.render(shadowLightShader, GL20.GL_TRIANGLES, 0, 6)

			shadowLightMesh.unbind(shadowLightShader)
		}

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)

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

	private fun renderVertices()
	{
		if (currentBuffer == null && staticBuffers.size == 0 && queuedBuffers.size == 0) return

		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glDepthMask(false)
		shader.begin()

		shader.setUniformMatrix("u_projTrans", combinedMatrix)
		shader.setUniformf("u_offset", offsetx, offsety)
		shader.setUniformi("u_texture", 0)
		shader.setUniformi("u_lightTexture", 1)
		shader.setUniformf("u_lightTextureSize", lightFBOSize)

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

			mesh.render(shader, GL20.GL_TRIANGLES, 0, 6)
		}

		if (staticBuffers.size > 0 && renderStatic)
		{
			staticGeometryMesh.bind(shader)

			for (buffer in staticBuffers)
			{
				drawBuffer(buffer, staticGeometryMesh)
			}

			staticGeometryMesh.unbind(shader)
		}

		if (queuedBuffers.size > 0)
		{
			for (buffer in queuedBuffers)
			{
				geometryMesh.setInstanceData(geometryInstanceData, buffer.offset, buffer.count)
				geometryMesh.bind(shader)

				drawBuffer(buffer, geometryMesh)
				bufferPool.free(buffer)

				geometryMesh.unbind(shader)
			}
			queuedBuffers.clear()
		}

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
		shader.end()

		currentGeometryInstanceIndex = 0
	}

	internal fun draw(batch: Batch?)
	{
		if (renderer.queuedSprites == 0 && staticBuffers.size == 0)
		{
			return
		}

		if (batch != null) combinedMatrix.set(batch.projectionMatrix).mul(batch.transformMatrix)

		if (!renderer.inStaticBegin)
		{
			updateLightBuffer()
		}

		// begin rendering
		for (i in 0 until renderer.queuedSprites)
		{
			val rs = renderer.spriteArray[i]!!

			fillVertexBuffer(rs)

			if (!renderer.inStaticBegin && currentGeometryInstanceIndex + instanceDataSize >= maxInstances * instanceDataSize)
			{
				renderVertices()
			}
		}

		if (renderer.inStaticBegin)
		{
			storeStatic()
		}
		else
		{
			renderVertices()

			//batch?.begin()
			//batch?.draw(lightFBO.colorBufferTexture, 0f, 0f, Gdx.graphics.backBufferWidth.toFloat(), Gdx.graphics.backBufferHeight.toFloat())
			//batch?.end()
		}
	}

	companion object
	{
		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		public const val instanceDataSize = 4+4+4+1+1+1
		private const val maxInstances = 100000

		fun createShader(): ShaderProgram
		{
			val vertexShader = getGeometryVertex()
			val fragmentShader = getGeometryFragment()

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			return shader
		}

		fun createLightShader(): ShaderProgram
		{
			val vertexShader = getLightVertex()
			val fragmentShader = getLightFragment()

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			return shader
		}

		fun createShadowLightShader(): ShaderProgram
		{
			val vertexShader = getShadowLightVertex()
			val fragmentShader = getShadowLightFragment()

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			return shader
		}

		fun getLightVertex(): String
		{
			return """
#version 300 es
// per vertex
in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};

// per instance
in vec4 a_pos_range_brightness;
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};

// uniforms
uniform mat4 u_projTrans;
uniform vec2 u_offset;

// outputs
out vec4 v_color;
out vec2 v_lightPos;
out vec2 v_pixelPos;
out float v_lightRange;
out float v_brightness;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy * a_pos_range_brightness.z + a_pos_range_brightness.xy;
	vec4 viewPos = vec4(worldPos.xy + u_offset, 0.0, 1.0);

	v_pixelPos = worldPos;
	v_lightPos = a_pos_range_brightness.xy;
	
	v_lightRange = a_pos_range_brightness.z;
	v_brightness = a_pos_range_brightness.w;
	
	gl_Position = u_projTrans * viewPos;
}
			""".trimIndent()
		}

		fun getLightFragment(): String
		{
			return """
#version 300 es
in lowp vec4 v_color;
in mediump vec2 v_lightPos;
in mediump vec2 v_pixelPos;
in lowp float v_lightRange;
in lowp float v_brightness;

out lowp vec4 fragColour;

lowp float calculateLightStrength()
{
	mediump vec2 diff = v_lightPos - v_pixelPos;
	lowp float distSq = (diff.x * diff.x) + (diff.y * diff.y);
	lowp float rangeSq = v_lightRange * v_lightRange;

	lowp float lightStrength = step(distSq, rangeSq);
	lowp float alpha = 1.0 - (distSq / rangeSq);

	return lightStrength * alpha;
}

void main()
{
	lowp float lightStrength = calculateLightStrength();
	fragColour = v_color * v_brightness * lightStrength;
}
			""".trimIndent()
		}

		fun getShadowLightVertex(): String
		{
			return """
#version 300 es
// per vertex
in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};

// per instance
in vec4 a_pos_range_brightness;
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec2 a_region_offset_count;

// uniforms
uniform mat4 u_projTrans;
uniform vec2 u_offset;

// outputs
out vec4 v_color;
out vec2 v_lightPos;
out vec2 v_pixelPos;
out float v_lightRange;
out float v_brightness;
out vec2 v_region_offset_count;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy * a_pos_range_brightness.z + a_pos_range_brightness.xy;
	vec4 viewPos = vec4(worldPos.xy + u_offset, 0.0, 1.0);

	v_pixelPos = worldPos;
	v_lightPos = a_pos_range_brightness.xy;
	
	v_lightRange = a_pos_range_brightness.z;
	v_brightness = a_pos_range_brightness.w;
	
	v_region_offset_count = a_region_offset_count;
	
	gl_Position = u_projTrans * viewPos;
}
			""".trimIndent()
		}

		fun getShadowLightFragment(): String
		{
			return """
#version 300 es
in lowp vec4 v_color;
in mediump vec2 v_lightPos;
in mediump vec2 v_pixelPos;
in lowp float v_lightRange;
in lowp float v_brightness;
in lowp vec2 v_region_offset_count;

uniform lowp vec4 u_shadowRegions[128];

out lowp vec4 fragColour;

// ------------------------------------------------------
lowp float rayBoxIntersect ( lowp vec2 rpos, lowp vec2 rdir, lowp vec2 vmin, lowp vec2 vmax )
{
	lowp float t0 = (vmin.x - rpos.x) * rdir.x;
	lowp float t1 = (vmax.x - rpos.x) * rdir.x;
	lowp float t2 = (vmin.y - rpos.y) * rdir.y;
	lowp float t3 = (vmax.y - rpos.y) * rdir.y;

	lowp float t4 = max(min(t0, t1), min(t2, t3));
	lowp float t5 = min(max(t0, t1), max(t2, t3));

	lowp float t6 = (t5 < 0.0 || t4 > t5) ? -1.0 : t4;
	return t6;
}

// ------------------------------------------------------
lowp float insideBox(lowp vec2 v, lowp vec2 bottomLeft, lowp vec2 topRight)
{
    lowp vec2 s = step(bottomLeft, v) - step(topRight, v);
    return s.x * s.y;
}

// ------------------------------------------------------
lowp float isPixelVisible()
{
	mediump vec2 diff = v_lightPos - v_pixelPos;
	lowp float rayLen = length(diff);
	lowp vec2 rdir = 1.0 / (diff / rayLen);

	lowp float collided = 0.0;
	for (int i = 0; i < int(v_region_offset_count.y); i++)
	{
		lowp vec4 occluder = u_shadowRegions[int(v_region_offset_count.x) + i];
		lowp float intersect = rayBoxIntersect(v_pixelPos, rdir, occluder.xy, occluder.zw);

		collided += float(intersect > 0.0 && intersect < rayLen && insideBox(v_pixelPos, occluder.xy, occluder.zw) == 0.0);
	}

	return float(collided == 0.0);
}

// ------------------------------------------------------
lowp float calculateLightStrength()
{
	mediump vec2 diff = v_lightPos - v_pixelPos;
	lowp float distSq = (diff.x * diff.x) + (diff.y * diff.y);
	lowp float rangeSq = v_lightRange * v_lightRange;

	lowp float lightStrength = step(distSq, rangeSq);
	lowp float alpha = 1.0 - (distSq / rangeSq);
	
	lowp float isVisible = isPixelVisible();

	return lightStrength * alpha * isVisible;
}

// ------------------------------------------------------
void main()
{
	lowp float lightStrength = calculateLightStrength();
	fragColour = v_color * v_brightness * lightStrength;
}
			""".trimIndent()
		}

		fun getGeometryVertex(): String
		{
			val vertexShader = """
#version 300 es
#define PI 3.1415926538

// per vertex
in vec2 ${ShaderProgram.POSITION_ATTRIBUTE};

// per instance
in vec4 a_pos_width_height;
in vec4 a_texCoords0;
in vec4 a_texCoords1;
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec4 a_blendAlpha_isLit_alphaRef_rotation;
in float a_smoothLighting;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

out vec4 v_color;
out vec2 v_lightSamplePos;

out vec2 v_texCoords1;
out vec2 v_texCoords2;

out float v_blendAlpha;
out float v_isLit;
out float v_alphaRef;

void main()
{
	vec2 vertexPos = ${ShaderProgram.POSITION_ATTRIBUTE};
	
	float rotation = a_blendAlpha_isLit_alphaRef_rotation.w * PI * 2.0;
	float c = cos(rotation);
	float s = sin(rotation);

	vec2 rotatedVertexPos = vec2(vertexPos.x * c - vertexPos.y * s, vertexPos.x * s + vertexPos.y * c);

	vec2 halfSize = a_pos_width_height.zw * 0.5;
	vec2 worldPos = rotatedVertexPos * halfSize + a_pos_width_height.xy;
	vec2 basePos = a_pos_width_height.xy - vec2(0.0, halfSize.y * 0.8);
	vec4 viewPos = vec4(worldPos.xy + u_offset, 0.0, 1.0);
	vec4 screenPos = u_projTrans * viewPos;
	vec4 baseScreenPos = u_projTrans * vec4(basePos.xy + u_offset, 0.0, 1.0);

	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
	v_lightSamplePos = mix((baseScreenPos.xy + 1.0) / 2.0, (screenPos.xy + 1.0) / 2.0, a_smoothLighting);
	
	vec2 texCoordAlpha = (vertexPos + 1.0) / 2.0;
	v_texCoords1 = mix(a_texCoords0.xy, a_texCoords0.zw, texCoordAlpha);
	v_texCoords2 = mix(a_texCoords1.xy, a_texCoords1.zw, texCoordAlpha);
	
	v_blendAlpha = a_blendAlpha_isLit_alphaRef_rotation.x;
	v_isLit = float(a_blendAlpha_isLit_alphaRef_rotation.y == 0.0);
	v_alphaRef = a_blendAlpha_isLit_alphaRef_rotation.z;
	
	gl_Position = screenPos;
}
""".trimIndent()

			return vertexShader
		}

		fun getGeometryFragment(): String
		{
			val fragmentShader = """
#version 300 es

in lowp vec4 v_color;
in mediump vec2 v_lightSamplePos;

in mediump vec2 v_texCoords1;
in mediump vec2 v_texCoords2;

in lowp float v_blendAlpha;
in lowp float v_isLit;
in lowp float v_alphaRef;

uniform sampler2D u_lightTexture;
uniform lowp vec2 u_lightTextureSize;

uniform sampler2D u_texture;

out lowp vec4 fragColour;

// ------------------------------------------------------
void main()
{
	lowp vec3 light = texture(u_lightTexture, v_lightSamplePos).rgb * 0.4;
	
	lowp vec2 sampleOffset = 1.0 / u_lightTextureSize;
	light += texture(u_lightTexture, v_lightSamplePos + vec2(-sampleOffset.x, -sampleOffset.y)).rgb * 0.15;
	light += texture(u_lightTexture, v_lightSamplePos + vec2(sampleOffset.x, sampleOffset.y)).rgb * 0.15;
	light += texture(u_lightTexture, v_lightSamplePos + vec2(-sampleOffset.x, sampleOffset.y)).rgb * 0.15;
	light += texture(u_lightTexture, v_lightSamplePos + vec2(sampleOffset.x, -sampleOffset.y)).rgb * 0.15;
	
	light = mix(vec3(1.0, 1.0, 1.0), light, v_isLit);
	
	lowp vec4 col1 = texture(u_texture, v_texCoords1);
	lowp vec4 col2 = texture(u_texture, v_texCoords2);

	lowp vec4 outCol = mix(col1, col2, v_blendAlpha);
	outCol *= step(v_alphaRef, v_color.a * outCol.a);

	fragColour = clamp(v_color * outCol, 0.0, 1.0) * vec4(light.rgb, 1.0);
}
""".trimIndent()
			return fragmentShader
		}
	}
}