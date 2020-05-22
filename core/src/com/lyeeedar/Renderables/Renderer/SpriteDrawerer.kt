package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BigMesh
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.GL30FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Particle.Particle
import com.lyeeedar.Renderables.doDraw
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

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

	private val lightMesh: BigMesh
	private val lightShader: ShaderProgram
	private val lightFBO: GL30FrameBuffer
	private var numLights: Int = 0

	private val mesh: BigMesh
	private val staticMesh: BigMesh
	private var currentBuffer: VertexBuffer? = null
	private val vertices: FloatArray
	private var currentVertexCount = 0
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
		mesh = BigMesh(false, maxSprites * 4, maxSprites * 6,
		               VertexAttribute(VertexAttributes.Usage.Position, 4, ShaderProgram.POSITION_ATTRIBUTE),
		               VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 4, ShaderProgram.TEXCOORD_ATTRIBUTE),
		               VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		               VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_additionalData") // blendalpha, islit, alpharef, brightness
		              )

		staticMesh = BigMesh(true, maxSprites * 4, maxSprites * 6,
		                     VertexAttribute(VertexAttributes.Usage.Position, 4, ShaderProgram.POSITION_ATTRIBUTE),
		                     VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 4, ShaderProgram.TEXCOORD_ATTRIBUTE),
		                     VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		                     VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_additionalData") // blendalpha, islit, alpharef, brightness
		                    )

		lightMesh = BigMesh(false, maxSprites * 4, maxSprites * 6,
		               VertexAttribute(VertexAttributes.Usage.Position, 4, ShaderProgram.POSITION_ATTRIBUTE),
		               VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
		               VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_rangeBrightness")
		              )

		val len = maxSprites * 6
		val indices = IntArray(len)
		var j = 0
		var i = 0
		while (i < len)
		{
			indices[i] = j
			indices[i + 1] = j + 1
			indices[i + 2] = j + 2
			indices[i + 3] = j + 2
			indices[i + 4] = j + 3
			indices[i + 5] = j
			i += 6
			j += 4
		}
		mesh.setIndices(indices)
		staticMesh.setIndices(indices)
		lightMesh.setIndices(indices)

		vertices = FloatArray(maxVertices)

		shader = createShader()
		lightShader = createLightShader()

		lightFBO = GL30FrameBuffer(GL30.GL_RGB16F, GL30.GL_RGB, GL30.GL_FLOAT, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight, false)
	}

	override fun dispose()
	{
		mesh.dispose()
		staticMesh.dispose()
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
		if (currentVertexCount+verticesASprite >= maxVertices)
		{
			System.err.println("Too many vertices queued!")
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
			currentBuffer!!.offset = currentVertexCount
		}

		var buffer = currentBuffer!!
		if (buffer.blendSrc != blendSrc || buffer.blendDst != blendDst || buffer.texture != texture)
		{
			queuedBuffers.add(currentBuffer)
			buffer = bufferPool.obtain()
			buffer.reset(blendSrc, blendDst, texture)
			buffer.offset = currentVertexCount

			currentBuffer = buffer
		}

		val offset = currentVertexCount
		buffer.count += verticesASprite
		currentVertexCount += verticesASprite

		val localx = rs.x
		val localy = rs.y
		val localw = rs.width * renderer.tileSize
		val localh = rs.height * renderer.tileSize

		val colour = rs.colour

		if (sprite != null)
		{
			val renderCol = sprite.getRenderColour()
			if (!renderCol.isWhite()) colour.mul(renderCol)

			sprite.render(vertices, offset, colour, localx, localy, localw, localh, rs.scaleX, rs.scaleY, rs.rotation, rs.isLit)
		}
		else if (rs.texture != null)
		{
			doDraw(vertices, offset,
			       rs.texture!!, rs.nextTexture ?: rs.texture!!, colour,
			       localx, localy, 0.5f, 0.5f, 1f, 1f, localw * rs.scaleX, localh * rs.scaleY, rs.rotation, rs.flipX, rs.flipY,
			       0f, rs.blendAlpha, rs.alphaRef, rs.isLit, false)
		}
	}

	private fun fillLightBuffer()
	{
		var i = 0
		for (light in renderer.basicLights)
		{
			val x = light.pos.x * renderer.tileSize
			val y = light.pos.y * renderer.tileSize
			val range = light.range * renderer.tileSize
			val colourBrightness = light.colour.toScaledFloatBits()

			vertices[i++] = x - range
			vertices[i++] = y + range
			vertices[i++] = x
			vertices[i++] = y
			vertices[i++] = colourBrightness.x
			vertices[i++] = range
			vertices[i++] = colourBrightness.y * light.brightness

			vertices[i++] = x - range
			vertices[i++] = y - range
			vertices[i++] = x
			vertices[i++] = y
			vertices[i++] = colourBrightness.x
			vertices[i++] = range
			vertices[i++] = colourBrightness.y * light.brightness

			vertices[i++] = x + range
			vertices[i++] = y - range
			vertices[i++] = x
			vertices[i++] = y
			vertices[i++] = colourBrightness.x
			vertices[i++] = range
			vertices[i++] = colourBrightness.y * light.brightness

			vertices[i++] = x + range
			vertices[i++] = y + range
			vertices[i++] = x
			vertices[i++] = y
			vertices[i++] = colourBrightness.x
			vertices[i++] = range
			vertices[i++] = colourBrightness.y * light.brightness
		}

		lightMesh.setVertices(vertices, 0, i)
		numLights = renderer.basicLights.size
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

		val drawCount = numLights * 6
		lightMesh.render(lightShader, GL20.GL_TRIANGLES, 0, drawCount)

		lightMesh.unbind(lightShader)

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)

		lightFBO.end()
	}

	private fun storeStatic()
	{
		queuedBuffers.add(currentBuffer!!)
		currentBuffer = null

		staticBuffers.addAll(queuedBuffers)
		queuedBuffers.clear()

		staticMesh.setVertices(vertices, 0, currentVertexCount)
		currentVertexCount = 0
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
		var currentOffset = 0

		fun drawBuffer(buffer: VertexBuffer, mesh: BigMesh)
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

			val spritesInBuffer = buffer.count / (4 * vertexSize)
			val drawCount = spritesInBuffer * 6
			mesh.render(shader, GL20.GL_TRIANGLES, currentOffset, drawCount)
			currentOffset += drawCount
		}

		if (staticBuffers.size > 0 && renderStatic)
		{
			staticMesh.bind(shader)

			for (buffer in staticBuffers)
			{
				drawBuffer(buffer, staticMesh)
			}

			staticMesh.unbind(shader)
		}

		if (queuedBuffers.size > 0)
		{
			currentOffset = 0
			mesh.setVertices(vertices, 0, currentVertexCount)
			mesh.bind(shader)

			for (buffer in queuedBuffers)
			{
				drawBuffer(buffer, mesh)
				bufferPool.free(buffer)
			}
			queuedBuffers.clear()

			mesh.unbind(shader)
		}

		Gdx.gl.glDepthMask(true)
		Gdx.gl.glDisable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
		shader.end()

		currentVertexCount = 0
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
			fillLightBuffer()
			renderLights()
		}

		// begin rendering
		for (i in 0 until renderer.queuedSprites)
		{
			val rs = renderer.spriteArray[i]!!

			fillVertexBuffer(rs)

			if (!renderer.inStaticBegin && currentVertexCount+verticesASprite >= maxVertices)
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

		//batch?.begin()
		//batch?.draw(lightFBO.colorBufferTexture, 0f, 0f)
		//batch?.end()
	}

	companion object
	{
		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		public const val vertexSize = 4 + 4 + 1 + 1
		private const val maxSprites = 10000
		public const val verticesASprite = vertexSize * 4
		private const val maxVertices = maxSprites * vertexSize

		fun createShader(): ShaderProgram
		{
			val vertexShader = getVertexUnoptimised()
			val fragmentShader = getFragmentUnoptimised()

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
in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec2 a_rangeBrightness;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

out vec4 v_color;
out vec2 v_lightPos;
out vec2 v_pixelPos;
out float v_lightRange;
out float v_brightness;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos = vec4(worldPos.x, worldPos.y, 0.0, 1.0);

	v_pixelPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy;
	v_lightPos = ${ShaderProgram.POSITION_ATTRIBUTE}.zw;
	v_lightRange = a_rangeBrightness.x;
	v_brightness = a_rangeBrightness.y;
	gl_Position = u_projTrans * truePos;
}
			""".trimIndent()
		}

		fun getLightFragment(): String
		{
			return """
#version 300 es
in mediump vec4 v_color;
in mediump vec2 v_lightPos;
in mediump vec2 v_pixelPos;
in mediump float v_lightRange;
in mediump float v_brightness;

out highp vec4 fragColour;

mediump float calculateLightStrength()
{
	mediump vec2 diff = v_lightPos - v_pixelPos;
	mediump float distSq = (diff.x * diff.x) + (diff.y * diff.y);
	mediump float rangeSq = v_lightRange * v_lightRange;

	mediump float lightStrength = step(distSq, rangeSq);
	mediump float alpha = 1.0 - (distSq / rangeSq);

	return lightStrength * alpha;
}

void main()
{
	mediump float lightStrength = calculateLightStrength();
	fragColour = v_color * v_brightness * lightStrength;
}
			""".trimIndent()
		}

		fun getVertexUnoptimised(): String
		{
			val vertexShader = """
#version 300 es
in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
in vec4 ${ShaderProgram.TEXCOORD_ATTRIBUTE};
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec4 a_additionalData;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

out vec4 v_color;
out vec2 v_spritePos;
out vec2 v_texCoords1;
out vec2 v_texCoords2;
out float v_blendAlpha;
out float v_isLit;
out float v_alphaRef;
out float v_brightness;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos = vec4(worldPos.x, worldPos.y, 0.0, 1.0);
	vec4 screenPos = u_projTrans * truePos;

	v_spritePos = (screenPos.xy + 1.0) / 2.0;
	v_texCoords1 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.xy;
	v_texCoords2 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.zw;
	v_blendAlpha = a_additionalData.x;
	v_isLit = float(a_additionalData.y == 0.0);
	v_alphaRef = a_additionalData.z;
	v_brightness = a_additionalData.w;
	gl_Position = screenPos;
}
""".trimIndent()

			return vertexShader
		}

		fun getFragmentUnoptimised(): String
		{
			val fragmentShader = """
#version 300 es
in mediump vec4 v_color;
in mediump vec2 v_spritePos;
in mediump vec2 v_texCoords1;
in mediump vec2 v_texCoords2;
in mediump float v_blendAlpha;
in mediump float v_isLit;
in mediump float v_alphaRef;
in mediump float v_brightness;

uniform sampler2D u_lightTexture;
uniform sampler2D u_texture;

out highp vec4 fragColour;

// ------------------------------------------------------
void main()
{
	highp vec4 light = texture(u_lightTexture, v_spritePos);
	highp vec4 col1 = texture(u_texture, v_texCoords1);
	highp vec4 col2 = texture(u_texture, v_texCoords2);

	highp vec4 outCol = mix(col1, col2, v_blendAlpha);

	if (v_color.a * outCol.a < v_alphaRef)
	{
		outCol = vec4(0.0, 0.0, 0.0, 0.0);
	}

	highp vec4 objCol = vec4(v_color.rgb * v_brightness * 255.0, v_color.a);

	highp vec4 finalCol = clamp(objCol * outCol, 0.0, 1.0);
	fragColour = finalCol * vec4(light.rgb, 1.0);
}
""".trimIndent()
			return fragmentShader
		}
	}
}