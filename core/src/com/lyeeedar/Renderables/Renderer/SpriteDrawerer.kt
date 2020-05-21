package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BigMesh
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Direction
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

	private val mesh: BigMesh
	private val staticMesh: BigMesh
	private var currentBuffer: VertexBuffer? = null
	private val vertices: FloatArray
	private var currentVertexCount = 0
	private val staticBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private val queuedBuffers = com.badlogic.gdx.utils.Array<VertexBuffer>()
	private lateinit var shader: ShaderProgram
	private var shaderLightNum: Int = 0
	private var shaderShadowLightNum: Int = 0
	private var shaderRegionsPerLight: IntArray = IntArray(0)
	private var lightPosRange: FloatArray
	private var lightColourBrightness: FloatArray
	private var lightShadowPosRange: FloatArray
	private var lightShadowColourBrightness: FloatArray
	private var lightShadowRegions: FloatArray
	private val combinedMatrix: Matrix4 = Matrix4()
	private var regionsPerLight = IntArray(shaderShadowLightNum)
	private val ambientLightVec = Vector3()
	private val bitflag = EnumBitflag<Direction>()

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

		vertices = FloatArray(maxVertices)

		shaderLightNum = 10
		lightPosRange = FloatArray(shaderLightNum * 3)
		lightColourBrightness = FloatArray(shaderLightNum * 4)
		lightShadowPosRange = FloatArray(0)
		lightShadowColourBrightness = FloatArray(0)
		lightShadowRegions = FloatArray(0)
		shader = createShader(shaderLightNum, shaderShadowLightNum, IntArray(0))
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

	private inline fun requestRender(rs: RenderSprite)
	{
		if (currentVertexCount+verticesASprite >= maxVertices)
		{
			System.err.println("Too many vertices queued!")
			return
		}

		val blendSrc = rs.blend.src
		val blendDst = rs.blend.dst

		var sprite = rs.sprite
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

			sprite = rs.tilingSprite!!.getSprite(bitflag)
			if (sprite.light != null)
			{
				renderer.addLight(sprite.light!!, rs.px + 0.5f, rs.py + 0.5f)
			}
		}
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

	private fun storeStatic()
	{
		queuedBuffers.add(currentBuffer!!)
		currentBuffer = null

		staticBuffers.addAll(queuedBuffers)
		queuedBuffers.clear()

		staticMesh.setVertices(vertices, 0, currentVertexCount)
		currentVertexCount = 0
	}

	private fun waitOnRender()
	{
		if (currentBuffer == null && staticBuffers.size == 0 && queuedBuffers.size == 0) return

		val basicLights = renderer.basicLights
		val shadowLights = renderer.shadowLights
		val tileSize = renderer.tileSize
		val offsetx = renderer.offsetx
		val offsety = renderer.offsety

		var rebuildShader = false

		if (renderer.basicLights.size > shaderLightNum)
		{
			shaderLightNum = basicLights.size
			rebuildShader = true

			lightPosRange = FloatArray(shaderLightNum * 3)
			lightColourBrightness = FloatArray(shaderLightNum * 4)
		}

		val sortedShadowLights = shadowLights.filter { it.cache.anyClear() }.sortedBy { if (shadowMode == ShadowMode.TILE) it.cache.numCastRegions else it.cache.numOpaqueRegions }.toGdxArray()

		if (sortedShadowLights.size != shaderShadowLightNum)
		{
			shaderShadowLightNum = sortedShadowLights.size
			rebuildShader = true

			val shadowPosRangeSize = if (shadowMode == ShadowMode.TILE) 4 else 3
			lightShadowPosRange = FloatArray(shaderShadowLightNum * shadowPosRangeSize)
			lightShadowColourBrightness = FloatArray(shaderShadowLightNum * 4)
		}

		if (regionsPerLight.size != shaderShadowLightNum)
		{
			regionsPerLight = IntArray(shaderShadowLightNum)
		}

		for (regionI in regionsPerLight.indices)
		{
			regionsPerLight[regionI] = 0
		}

		var regionsDifferent = false
		if (Statics.lightCollisionGrid != null)
		{
			for (i in 0 until sortedShadowLights.size)
			{
				val light = sortedShadowLights[i]

				val numCount: Int
				if (shadowMode == ShadowMode.TILE)
				{
					numCount = light.cache.numCastRegions
				}
				else
				{
					numCount = light.cache.numOpaqueRegions
				}

				if (i < shaderRegionsPerLight.size)
				{
					val shaderNumCount = shaderRegionsPerLight[i]
					if (numCount > shaderNumCount || numCount < shaderNumCount * 0.75f)
					{
						regionsDifferent = true
					}
				}

				regionsPerLight[i] = numCount
			}
		}

		if (regionsPerLight.size != shaderRegionsPerLight.size || regionsDifferent)
		{
			rebuildShader = true
			shaderRegionsPerLight = regionsPerLight
			lightShadowRegions = FloatArray(regionsPerLight.sum() * 4)
		}

		var i = 0
		for (lightI in 0 until basicLights.size)
		{
			val light = basicLights[lightI]

			lightPosRange[(i*3)+0] = light.pos.x * tileSize + offsetx
			lightPosRange[(i*3)+1] = light.pos.y * tileSize + offsety
			lightPosRange[(i*3)+2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)

			lightColourBrightness[(i*4)+0] = light.colour.r
			lightColourBrightness[(i*4)+1] = light.colour.g
			lightColourBrightness[(i*4)+2] = light.colour.b
			lightColourBrightness[(i*4)+3] = light.brightness

			i++
		}

		while (i < shaderLightNum)
		{
			lightPosRange[(i*3)+0] = -1f
			lightPosRange[(i*3)+1] = -1f
			lightPosRange[(i*3)+2] = -1f

			i++
		}

		var shadowCacheOffset = 0
		i = 0
		for (lightI in 0 until sortedShadowLights.size)
		{
			val light = sortedShadowLights[lightI]

			lightShadowColourBrightness[(i*4)+0] = light.colour.r
			lightShadowColourBrightness[(i*4)+1] = light.colour.g
			lightShadowColourBrightness[(i*4)+2] = light.colour.b
			lightShadowColourBrightness[(i*4)+3] = light.brightness

			val regionsToSet: com.badlogic.gdx.utils.Array<PointRect>
			if (shadowMode == ShadowMode.TILE)
			{
				regionsToSet = light.cache.getCurrentCastRegions()

				lightShadowPosRange[(i * 4) + 0] = light.pos.x * tileSize + offsetx
				lightShadowPosRange[(i * 4) + 1] = light.pos.y * tileSize + offsety
				lightShadowPosRange[(i * 4) + 2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)
				lightShadowPosRange[(i * 4) + 3] = if (light.cache.regionsVisible) 2.0f else -2.0f
			}
			else
			{
				regionsToSet = light.cache.getOpaqueRegions()

				lightShadowPosRange[(i * 3) + 0] = light.pos.x * tileSize + offsetx
				lightShadowPosRange[(i * 3) + 1] = light.pos.y * tileSize + offsety
				lightShadowPosRange[(i * 3) + 2] = (light.range * tileSize * 0.9f) * (light.range * tileSize * 0.9f)
			}

			val cacheStart = shadowCacheOffset
			var minx = 0f
			var miny = 0f
			var maxx = 0f
			var maxy = 0f
			for (regionI in 0 until regionsToSet.size)
			{
				val region = regionsToSet[regionI]

				minx = region.x * tileSize
				miny = region.y * tileSize
				maxx = minx + region.width * tileSize
				maxy = miny + region.height * tileSize

				lightShadowRegions[shadowCacheOffset++] = minx
				lightShadowRegions[shadowCacheOffset++] = miny
				lightShadowRegions[shadowCacheOffset++] = maxx
				lightShadowRegions[shadowCacheOffset++] = maxy
			}

			while (shadowCacheOffset < cacheStart + shaderRegionsPerLight[i] * 4)
			{
				lightShadowRegions[shadowCacheOffset++] = minx
				lightShadowRegions[shadowCacheOffset++] = miny
				lightShadowRegions[shadowCacheOffset++] = maxx
				lightShadowRegions[shadowCacheOffset++] = maxy
			}

			i++
		}

		while (i < shaderShadowLightNum)
		{
			if (shadowMode == ShadowMode.TILE)
			{
				lightShadowPosRange[(i * 4) + 0] = -1f
				lightShadowPosRange[(i * 4) + 1] = -1f
				lightShadowPosRange[(i * 4) + 2] = -1f
				lightShadowPosRange[(i * 4) + 3] = -1f
			}
			else
			{
				lightShadowPosRange[(i * 3) + 0] = -1f
				lightShadowPosRange[(i * 3) + 1] = -1f
				lightShadowPosRange[(i * 3) + 2] = -1f
			}

			i++
		}

		if (rebuildShader)
		{
			shader = createShader(shaderLightNum, shaderShadowLightNum, shaderRegionsPerLight)
		}

		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glDepthMask(false)
		shader.begin()

		ambientLightVec.set(ambientLight.r, ambientLight.g, ambientLight.b)

		shader.setUniformMatrix("u_projTrans", combinedMatrix)
		shader.setUniformf("u_offset", offsetx, offsety)
		shader.setUniformi("u_texture", 0)
		shader.setUniformf("u_ambient", ambientLightVec)

		if (shaderShadowLightNum > 0 || !smoothLighting)
		{
			shader.setUniformf("u_tileSize", tileSize)
		}

		shader.setUniform3fv("u_lightPosRange", lightPosRange, 0, lightPosRange.size)
		shader.setUniform4fv("u_lightColourBrightness", lightColourBrightness, 0, lightColourBrightness.size)

		if (shaderShadowLightNum > 0)
		{
			if (shadowMode == ShadowMode.TILE)
			{
				shader.setUniform4fv("u_shadowedLightPosRangeMode", lightShadowPosRange, 0, lightShadowPosRange.size)
			}
			else
			{
				shader.setUniform3fv("u_shadowedLightPosRange", lightShadowPosRange, 0, lightShadowPosRange.size)
			}

			shader.setUniform4fv("u_shadowedLightColourBrightness", lightShadowColourBrightness, 0, lightShadowColourBrightness.size)
			shader.setUniform4fv("u_shadowedLightRegions", lightShadowRegions, 0, lightShadowRegions.size)
		}

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
				buffer.texture.bind()
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

		// begin rendering
		for (i in 0 until renderer.queuedSprites)
		{
			val rs = renderer.spriteArray[i]!!

			requestRender(rs)

			if (!renderer.inStaticBegin && currentVertexCount+verticesASprite >= maxVertices)
			{
				waitOnRender()
			}
		}

		if (renderer.inStaticBegin)
		{
			storeStatic()
		}
		else
		{
			waitOnRender()
		}
	}

	companion object
	{
		enum class ShadowMode
		{
			NONE,
			TILE,
			SMOOTH
		}

		// Optimisation reference:
		// https://zz85.github.io/glsl-optimizer/

		private val smoothLighting = true
		private val shadowMode: ShadowMode = ShadowMode.TILE

		public const val vertexSize = 4 + 4 + 1 + 1
		private const val maxSprites = 10000
		public const val verticesASprite = vertexSize * 4
		private const val maxVertices = maxSprites * vertexSize

		private val cachedShaders = ObjectMap<String, ShaderProgram>()

		fun createShader(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): ShaderProgram
		{
			val key = numLights.toString() + numShadowLights.toString() + regionsPerLight.map { it.toString() }.joinToString()

			val existing = cachedShaders[key]
			if (existing != null)
			{
				return existing
			}

			val vertexShader = getVertexOptimised()
			val fragmentShader = getFragmentOptimised(numLights, numShadowLights, regionsPerLight)

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			cachedShaders[key] = shader

			return shader
		}

		fun getVertexUnoptimised(): String
		{
			val vertexShader = """
attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.TEXCOORD_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec4 a_additionalData;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

varying vec4 v_color;
varying vec2 v_spritePos;
varying vec2 v_pixelPos;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying float v_blendAlpha;
varying float v_isLit;
varying float v_alphaRef;
varying float v_brightness;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos = vec4(worldPos.x, worldPos.y, 0.0, 1.0);

	v_pixelPos = worldPos;
	v_spritePos = ${ShaderProgram.POSITION_ATTRIBUTE}.zw;
	v_texCoords1 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.xy;
	v_texCoords2 = ${ShaderProgram.TEXCOORD_ATTRIBUTE}.zw;
	v_blendAlpha = a_additionalData.x;
	v_isLit = float(a_additionalData.y == 0.0);
	v_alphaRef = a_additionalData.z;
	v_brightness = a_additionalData.w;
	gl_Position = u_projTrans * truePos;
}
"""

			return vertexShader
		}

		fun getVertexOptimised(): String
		{
			val androidDefine = if (Statics.android) "#define LOWP lowp" else "#define LOWP"

			val vertexShader = """
$androidDefine

attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.TEXCOORD_ATTRIBUTE};
attribute LOWP vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute LOWP vec4 a_additionalData;

uniform mat4 u_projTrans;
uniform vec2 u_offset;

varying LOWP vec4 v_color;
varying vec4 v_pos;
varying vec4 v_texCoords;
varying LOWP vec4 v_additionalData;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	vec2 worldPos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy + u_offset;
	vec4 truePos;
	truePos.xy = worldPos;
	truePos.zw = vec2(0.0, 1.0);

	v_pos.xy = worldPos;
	v_pos.zw = ${ShaderProgram.POSITION_ATTRIBUTE}.zw;
	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE};
	v_additionalData = a_additionalData;

	gl_Position = u_projTrans * truePos;
}
"""

			return vertexShader
		}

		fun getFragmentUnoptimised(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): String
		{
			val shadowDefine =
				if (numShadowLights > 0)
				{
					when (shadowMode)
					{
						ShadowMode.NONE -> ""
						ShadowMode.SMOOTH -> "#define SMOOTHSHADOWS 1"
						ShadowMode.TILE -> "#define TILESHADOWS 1"
					}
				}
				else ""

			var regionsDefine = "const vec2 regionSizes[${regionsPerLight.size}] = vec2[${regionsPerLight.size}]("

			var currentRegionIndex = 0
			for (region in regionsPerLight)
			{
				if (currentRegionIndex != 0)
				{
					regionsDefine += ", "
				}

				regionsDefine += "vec2($currentRegionIndex, $region)"
				currentRegionIndex += region
			}

			regionsDefine += ");"

			val tileLightingDefine = if (!smoothLighting) "#define TILELIGHTING 1" else ""
			val fragmentShader = """

$shadowDefine
$tileLightingDefine

varying vec4 v_color;
varying vec2 v_spritePos;
varying vec2 v_pixelPos;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying float v_blendAlpha;
varying float v_isLit;
varying float v_alphaRef;
varying float v_brightness;

uniform float u_tileSize;

uniform vec3 u_ambient;

uniform vec3 u_lightPosRange[$numLights];
uniform vec4 u_lightColourBrightness[$numLights];

#ifdef SMOOTHSHADOWS
uniform vec3 u_shadowedLightPosRange[$numShadowLights];
uniform vec4 u_shadowedLightColourBrightness[$numShadowLights];
$regionsDefine
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

#ifdef TILESHADOWS
uniform vec4 u_shadowedLightPosRangeMode[$numShadowLights];
uniform vec4 u_shadowedLightColourBrightness[$numShadowLights];
$regionsDefine
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

uniform sampler2D u_texture;

// ------------------------------------------------------
float calculateLightStrength(vec3 posRange)
{
	vec2 pos = posRange.xy;
	float rangeSq = posRange.z;

	vec2 pixelPos = v_pixelPos;

#ifdef TILELIGHTING
	pixelPos = (floor(v_spritePos / u_tileSize)) * u_tileSize;
	pos = (floor(pos / u_tileSize)) * u_tileSize;
#endif

	vec2 diff = pos - pixelPos;
	float distSq = (diff.x * diff.x) + (diff.y * diff.y);

	float lightStrength = step(distSq, rangeSq);
	float alpha = 1.0 - (distSq / rangeSq);

	return lightStrength * alpha;
}

// ------------------------------------------------------
vec3 calculateLight(int index)
{
	vec3 posRange = u_lightPosRange[index];
	vec4 colourBrightness = u_lightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRange);

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}

#ifdef TILESHADOWS

// ------------------------------------------------------
bool insideBox(vec2 v, vec2 bottomLeft, vec2 topRight)
{
    vec2 s = step(bottomLeft, v) - step(topRight, v);
    return s.x * s.y != 0.0;
}

// ------------------------------------------------------
bool isPointVisible(int index, vec2 point, bool regionsAreLit)
{
	bool inRegion = false;
	for (int i = 0; i < regionSizes[index].y; i++)
	{
		vec4 region = u_shadowedLightRegions[regionSizes[index].x + i];
		inRegion = inRegion || insideBox(point, region.xy, region.zw);
	}

	return (regionsAreLit == inRegion);
}

// ------------------------------------------------------
vec3 calculateShadowLight(int index)
{
	vec4 posRangeMode = u_shadowedLightPosRangeMode[index];
	vec4 colourBrightness = u_shadowedLightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRangeMode.xyz);

	vec2 pixelPos = v_pixelPos;
	pixelPos.y = v_spritePos.y + min(pixelPos.y - v_spritePos.y, u_tileSize*0.9);

	float multiplier = float(isPointVisible(index, pixelPos, posRangeMode.w > 0.0));

	lightStrength *= multiplier;

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}

#endif

#ifdef SMOOTHSHADOWS

// ------------------------------------------------------
float rayBoxIntersect ( vec2 rpos, vec2 rdir, vec2 vmin, vec2 vmax )
{
	float t0 = (vmin.x - rpos.x) * rdir.x;
	float t1 = (vmax.x - rpos.x) * rdir.x;
	float t2 = (vmin.y - rpos.y) * rdir.y;
	float t3 = (vmax.y - rpos.y) * rdir.y;

	float t4 = max(min(t0, t1), min(t2, t3));
	float t5 = min(max(t0, t1), max(t2, t3));

	float t6 = (t5 < 0.0 || t4 > t5) ? -1.0 : t4;
	return t6;
}

// ------------------------------------------------------
float insideBox(vec2 v, vec2 bottomLeft, vec2 topRight)
{
    vec2 s = step(bottomLeft, v) - step(topRight, v);
    return s.x * s.y;
}

// ------------------------------------------------------
bool isPointVisible(int index, vec2 point)
{
	vec3 posRange = u_shadowedLightPosRange[index];

	vec2 baseTile = (floor(v_spritePos / u_tileSize)) * u_tileSize;
	vec2 lightTile = (floor(posRange.xy / u_tileSize)) * u_tileSize;

	vec2 pixelPos = v_pixelPos;
	pixelPos.y = v_spritePos.y + min(pixelPos.y - v_spritePos.y, u_tileSize*0.9);

	vec2 diff = point - pixelPos;
	float rayLen = length(diff);
	vec2 rdir = 1.0 / (diff / rayLen);

	float collided = 0.0;
	float collidedOverride = 0.0;
	for (int i = 0; i < regionSizes[index].y; i++)
	{
		vec4 occluder = u_shadowedLightRegions[regionSizes[index].x + i];
		float intersect = rayBoxIntersect(pixelPos, rdir, occluder.xy, occluder.zw);

		collided += float(intersect > 0.0 && intersect < rayLen);

		occluder.xy = (floor(occluder.xy / u_tileSize)) * u_tileSize;
		collidedOverride += insideBox(baseTile, occluder.xy, occluder.zw);
	}

	return collided == 0.0 || collidedOverride > 0.0;
}

// ------------------------------------------------------
vec3 calculateShadowLight(int index)
{
	vec3 posRange = u_shadowedLightPosRange[index];
	vec4 colourBrightness = u_shadowedLightColourBrightness[index];

	float lightStrength = calculateLightStrength(posRange);

	float multiplier = float(isPointVisible(index, posRange.xy));

	lightStrength *= multiplier;

	vec3 lightCol = colourBrightness.rgb;
	float brightness = colourBrightness.a;

	return lightCol * brightness * lightStrength;
}
#endif

// ------------------------------------------------------
void main()
{
	vec4 col1 = texture2D(u_texture, v_texCoords1);
	vec4 col2 = texture2D(u_texture, v_texCoords2);

	vec4 outCol = mix(col1, col2, v_blendAlpha);

	vec3 lightCol = u_ambient;

	for (int i = 0; i < $numLights; i++)
	{
		lightCol += calculateLight(i);
	}

#ifdef SMOOTHSHADOWS
	for (int i = 0; i < $numShadowLights; i++)
	{
		lightCol += calculateShadowLight(i);
	}
#endif

#ifdef TILESHADOWS
	for (int i = 0; i < $numShadowLights; i++)
	{
		lightCol += calculateShadowLight(i);
	}
#endif

	lightCol = mix(vec3(1.0, 1.0, 1.0), lightCol, v_isLit);

	if (v_color.a * outCol.a < v_alphaRef)
	{
		outCol = vec4(0.0, 0.0, 0.0, 0.0);
	}

	vec4 objCol = vec4(v_color.rgb * v_brightness * 255.0, v_color.a);

	vec4 finalCol = clamp(objCol * outCol * vec4(lightCol, 1.0), 0.0, 1.0);
	gl_FragColor = finalCol;
}
"""

			return fragmentShader
		}

		fun getFragmentOptimised(numLights: Int, numShadowLights: Int, regionsPerLight: IntArray): String
		{
			val androidDefine = if (Statics.android) "#define LOWP lowp\nprecision mediump float;" else "#define LOWP"

			val shadowDefine =
				if (numShadowLights > 0)
				{
					when (shadowMode)
					{
						ShadowMode.NONE -> ""
						ShadowMode.SMOOTH -> "#define SMOOTHSHADOWS 1"
						ShadowMode.TILE -> "#define TILESHADOWS 1"
					}
				}
				else ""

			val tileLightingDefine = if (!smoothLighting) "#define TILELIGHTING 1" else ""
			var fragmentShader = """

$shadowDefine
$tileLightingDefine
$androidDefine

varying LOWP vec4 v_color;
varying vec4 v_pos;
varying vec4 v_texCoords;
varying LOWP vec4 v_additionalData;

uniform float u_tileSize;

uniform LOWP vec3 u_ambient;

uniform vec3 u_lightPosRange[$numLights];
uniform LOWP vec4 u_lightColourBrightness[$numLights];

#ifdef SMOOTHSHADOWS
uniform vec3 u_shadowedLightPosRange[$numShadowLights];
uniform LOWP vec4 u_shadowedLightColourBrightness[$numShadowLights];
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

#ifdef TILESHADOWS
uniform vec4 u_shadowedLightPosRangeMode[$numShadowLights];
uniform LOWP vec4 u_shadowedLightColourBrightness[$numShadowLights];
uniform vec4 u_shadowedLightRegions[${regionsPerLight.sum()}];
#endif

uniform sampler2D u_texture;

// ------------------------------------------------------
void main ()
{
	LOWP vec3 lightCol = u_ambient;

#ifdef TILELIGHTING
	vec2 spriteTile = floor(v_pos.zw / u_tileSize) * u_tileSize;
#endif

"""

			fragmentShader += "//########## Basic Lighting ##########//\n"
			for (i in 0 until numLights)
			{
				fragmentShader += """
	vec3 posRange_$i = u_lightPosRange[$i];
	LOWP vec4 colourBrightness_$i = u_lightColourBrightness[$i];

#ifdef TILELIGHTING
	vec2 diff_$i = posRange_$i.xy - spriteTile;
#else
	vec2 diff_$i = posRange_$i.xy - v_pos.xy;
#endif

	float len_$i = (diff_$i.x * diff_$i.x) + (diff_$i.y * diff_$i.y);
	lightCol = lightCol + (colourBrightness_$i.rgb * (colourBrightness_$i.a * (1.0 - (len_$i / posRange_$i.z)) * float(posRange_$i.z >= len_$i)));

"""
			}

			if (shadowMode == ShadowMode.SMOOTH)
			{
				if (numShadowLights > 0)
				{
					fragmentShader += """
//########## Shadow Lighting ##########//

	vec2 shadowPixelPos;
	shadowPixelPos.x = v_pixelPos.x;
	shadowPixelPos.y = (v_spritePos.y + min((v_pixelPos.y - v_spritePos.y), (u_tileSize * 0.9)));

				"""

					var regionIndex = 0
					for (li in 0 until numShadowLights)
					{
						fragmentShader += """
	vec3 shadowPosRange_$li = u_shadowedLightPosRange[$li];
	LOWP vec4 shadowColourBrightness_$li = u_shadowedLightColourBrightness[$li];

#ifdef TILELIGHTING
	vec2 shadowDiff_$li = (shadowPosRange_$li.xy - spriteTile);
#else
	vec2 shadowDiff_$li = (shadowPosRange_$li.xy - v_pos.xy);
#endif
	float shadowLen_$li = ((shadowDiff_$li.x * shadowDiff_$li.x) + (shadowDiff_$li.y * shadowDiff_$li.y));

	LOWP float shadowLightStrength_$li = (float((shadowPosRange_$li.z >= shadowLen_$li)) * (1.0 - (shadowLen_$li / shadowPosRange_$li.z)));

	vec2 rayDiff_$li = (shadowPosRange_$li.xy - shadowPixelPos);
	float rayLen_$li = sqrt(dot(rayDiff_$li, rayDiff_$li));

	LOWP vec2 rdir_$li = (1.0/((rayDiff_$li / rayLen_$li)));
	LOWP float collided_$li = 0.0;
	LOWP float collidedOverride_$li = 0.0;

	vec2 occluderPixel_${li};
	vec2 tmax_${li};
	vec2 tmin_${li};
						"""

						for (oi in 0 until regionsPerLight[li])
						{
							fragmentShader += """

	vec4 occluder_${li}_$oi = u_shadowedLightRegions[${regionIndex++}];

	tmin_${li} = (occluder_${li}_$oi.xy - shadowPixelPos) * rdir_$li;
	tmax_${li} = (occluder_${li}_$oi.zw - shadowPixelPos) * rdir_$li;

	float t4_${li}_$oi = max(min(tmin_${li}.x, tmax_${li}.x), min(tmin_${li}.y, tmax_${li}.y));
	float t5_${li}_$oi = min(max(tmin_${li}.x, tmax_${li}.x), max(tmin_${li}.y, tmax_${li}.y));

	LOWP float intersection_${li}_$oi = (t5_${li}_$oi < 0.0 || t4_${li}_$oi > t5_${li}_$oi) ? -1.0 : t4_${li}_$oi;

	collided_$li = collided_$li + float((intersection_${li}_$oi > 0.0) && (intersection_${li}_$oi < rayLen_$li));

	LOWP vec2 inBox_${li}_$oi = (vec2(greaterThanEqual(baseTile, occluder_${li}_$oi.xy)) - vec2(greaterThanEqual(baseTile, occluder_${li}_$oi.zw)));
	collidedOverride_$li = (collidedOverride_$li + (inBox_${li}_$oi.x * inBox_${li}_$oi.y));

	"""
						}

						fragmentShader += """
	shadowLightStrength_$li *= float(((collided_$li == 0.0) || (collidedOverride_$li > 0.0)));
	lightCol = (lightCol + ((shadowColourBrightness_$li.xyz * shadowColourBrightness_$li.w) * shadowLightStrength_$li));

						"""
					}
				}
			}
			else if (shadowMode == ShadowMode.TILE)
			{
				if (numShadowLights > 0)
				{
					fragmentShader += """
//########## Shadow Lighting ##########//

	vec2 shadowPixelPos;
	shadowPixelPos.x = v_pos.x;
	shadowPixelPos.y = v_pos.w + min(v_pos.y - v_pos.w, u_tileSize * 0.9);
	"""
					var regionIndex = 0
					for (li in 0 until numShadowLights)
					{
						fragmentShader += """
	vec4 shadowPosRangeMode_$li = u_shadowedLightPosRangeMode[$li];
	LOWP vec4 shadowColourBrightness_$li = u_shadowedLightColourBrightness[$li];

#ifdef TILELIGHTING
	vec2 shadowDiff_$li = shadowPosRangeMode_$li.xy - spriteTile;
#else
	vec2 shadowDiff_$li = shadowPosRangeMode_$li.xy - v_pos.xy;
#endif
	float shadowLen_$li = (shadowDiff_$li.x * shadowDiff_$li.x) + (shadowDiff_$li.y * shadowDiff_$li.y);

	LOWP float shadowLightStrength_$li = float(shadowPosRangeMode_$li.z >= shadowLen_$li) * (1.0 - (shadowLen_$li / shadowPosRangeMode_$li.z));

	LOWP float inRegion_$li = 0.0;
						"""

						for (oi in 0 until regionsPerLight[li])
						{
							fragmentShader += """

	vec4 region_${li}_$oi = u_shadowedLightRegions[${regionIndex++}];

	LOWP vec2 inBox_${li}_$oi = vec2(greaterThanEqual(shadowPixelPos, region_${li}_$oi.xy)) - vec2(greaterThanEqual(shadowPixelPos, region_${li}_$oi.zw));
	inRegion_$li = inRegion_$li + (inBox_${li}_$oi.x * inBox_${li}_$oi.y);

	"""
						}

						fragmentShader += """

	shadowLightStrength_$li = shadowLightStrength_$li * float(shadowPosRangeMode_$li.w > 0.0 == inRegion_$li > 0.0);
	lightCol = lightCol + (shadowColourBrightness_$li.xyz * (shadowColourBrightness_$li.w * shadowLightStrength_$li));

						"""
					}
				}
			}

			fragmentShader += """

//########## final composite ##########//
	LOWP vec4 lightCol4;
	lightCol4.rgb = mix(vec3(1.0, 1.0, 1.0), lightCol, 1.0 - v_additionalData.y);
	lightCol4.a = 1.0;

	LOWP vec4 objCol = vec4(v_color.rgb * (v_additionalData.w * 255.0), v_color.a);

	LOWP vec4 outCol = clamp(objCol * mix(texture2D(u_texture, v_texCoords.xy), texture2D(u_texture, v_texCoords.zw), v_additionalData.x) * lightCol4, 0.0, 1.0);
	outCol *= float(outCol.a > v_additionalData.z); // apply alpharef

	gl_FragColor = outCol;
}
				"""

			return fragmentShader
		}
	}
}