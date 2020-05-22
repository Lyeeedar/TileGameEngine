package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BigMesh
import com.badlogic.gdx.graphics.glutils.GL30FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
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
	private val lightShader: ShaderProgram
	private var lightFBO: GL30FrameBuffer
	private val lightInstanceData: FloatArray
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
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_blendAlpha_isLit_alphaRef_rotation"))

		staticGeometryMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		staticGeometryMesh.setVertices(billboardVertices)
		staticGeometryMesh.setIndices(billboardIndices)
		staticGeometryMesh.enableInstancedRendering(true, maxInstances,
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_width_height"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords0"),
		                                      VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_texCoords1"),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                                      VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_blendAlpha_isLit_alphaRef_rotation"))

		lightMesh = Mesh(true, 4, 6, VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE))
		lightMesh.setVertices(billboardVertices)
		lightMesh.setIndices(billboardIndices)
		lightMesh.enableInstancedRendering(false, 10000,
		                                   VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_pos_range_brightness"),
		                                   VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE))

		geometryInstanceData = FloatArray(maxInstances * instanceDataSize)
		lightInstanceData = FloatArray(10000 * (4 + 1))

		shader = createShader()
		lightShader = createLightShader()

		lightFBO = GL30FrameBuffer(GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, false)
	}

	override fun dispose()
	{
		geometryMesh.dispose()
		staticGeometryMesh.dispose()
		lightMesh.dispose()
		lightFBO.dispose()
		shader.dispose()
		lightShader.dispose()
	}

	fun updateFBO()
	{
		val width = Statics.stage.viewport.screenWidth
		val height = Statics.stage.viewport.screenHeight

		if (width != lightFBO.width || height != lightFBO.height)
		{
			lightFBO.dispose()
			lightFBO = GL30FrameBuffer(GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, width, height, false)
		}
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
			lightInstanceData[i++] = range
			lightInstanceData[i++] = colourBrightness.y * light.brightness
			lightInstanceData[i++] = colourBrightness.x
		}

		lightMesh.setInstanceData(lightInstanceData, 0, i)
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

		lightShader.begin()
		lightShader.setUniformMatrix("u_projTrans", combinedMatrix)
		lightShader.setUniformf("u_offset", offsetx, offsety)

		lightMesh.bind(lightShader)

		lightMesh.render(lightShader, GL20.GL_TRIANGLES, 0, 6)

		lightMesh.unbind(lightShader)

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
		}
	}

	companion object
	{
		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		public const val instanceDataSize = 4+4+4+1+1
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
in highp vec4 v_color;
in mediump vec2 v_lightPos;
in mediump vec2 v_pixelPos;
in mediump float v_lightRange;
in mediump float v_brightness;

out highp vec4 fragColour;

highp float calculateLightStrength()
{
	highp vec2 diff = v_lightPos - v_pixelPos;
	highp float distSq = (diff.x * diff.x) + (diff.y * diff.y);
	highp float rangeSq = v_lightRange * v_lightRange;

	highp float lightStrength = step(distSq, rangeSq);
	highp float alpha = 1.0 - (distSq / rangeSq);

	return lightStrength * alpha;
}

void main()
{
	highp float lightStrength = calculateLightStrength();
	fragColour = v_color * v_brightness * lightStrength;
}
			""".trimIndent()
		}

		fun getGeometryVertex(): String
		{
			val vertexShader = """
#version 300 es
// per vertex
in vec2 ${ShaderProgram.POSITION_ATTRIBUTE};

// per instance
in vec4 a_pos_width_height;
in vec4 a_texCoords0;
in vec4 a_texCoords1;
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec4 a_blendAlpha_isLit_alphaRef_rotation;

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
	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy * a_pos_width_height.zw * 0.5 + a_pos_width_height.xy;
	vec4 viewPos = vec4(worldPos.xy + u_offset, 0.0, 1.0);
	vec4 screenPos = u_projTrans * viewPos;

	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
	v_lightSamplePos = (screenPos.xy + 1.0) / 2.0;
	
	float texCoordAlpha = (${ShaderProgram.POSITION_ATTRIBUTE}.xy + 1.0) / 2.0;
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

in mediump vec4 v_color;
in mediump vec2 v_lightSamplePos;

in mediump vec2 v_texCoords1;
in mediump vec2 v_texCoords2;

in mediump float v_blendAlpha;
in mediump float v_isLit;
in mediump float v_alphaRef;

uniform sampler2D u_lightTexture;
uniform sampler2D u_texture;

out highp vec4 fragColour;

// ------------------------------------------------------
void main()
{
	highp vec4 light = texture(u_lightTexture, v_lightSamplePos);
	highp vec4 col1 = texture(u_texture, v_texCoords1);
	highp vec4 col2 = texture(u_texture, v_texCoords2);

	highp vec4 outCol = mix(col1, col2, v_blendAlpha);

	if (v_color.a * outCol.a < v_alphaRef)
	{
		outCol = vec4(0.0, 0.0, 0.0, 0.0);
	}

	highp vec4 finalCol = clamp(v_color * outCol, 0.0, 1.0);
	fragColour = finalCol * vec4(light.rgb, 1.0);
}
""".trimIndent()
			return fragmentShader
		}
	}
}