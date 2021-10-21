package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.CatmullRomSpline
import com.badlogic.gdx.math.Path
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Util.*

class CurveRenderable(val path: Path<Vector2>, val thicknessPixels: Float, val texture: TextureRegion, val samples: Int) : Renderable()
{
	private val vertexSize = 5
	val vertices: FloatArray = FloatArray(samples * 2 * vertexSize)
	val indices: ShortArray = ShortArray((samples - 1) * 2 * 3)
	private var offsetx: Float = Float.MAX_VALUE
	private var offsety: Float = Float.MAX_VALUE
	private var tileSize: Float = Float.MAX_VALUE
	private val lastCol: Colour = Colour.TRANSPARENT.copy()
	private var lastWindowStart = -1f
	private var lastWindowEnd = -1f

	private val sample = Vector2()
	private val lastSample = Vector2()
	private val dir = Vector2()
	private val realPos = Vector2()
	private var windowStart = 0f
	private var windowEnd = 1f

	private var windowEndAnimDuration = 0f
	private var windowSizeLag = 0f
	private var animTime = 0f
	var isAnimating = false

	fun setAnimation(duration: Float, lag: Float)
	{
		animTime = 0f
		windowEndAnimDuration = duration
		windowSizeLag = lag
		windowStart = 0f
		windowEnd = 0f
		isAnimating = true
	}

	fun completeAnimation()
	{
		windowEndAnimDuration = 0f
		animTime = 0f
		windowSizeLag = 0f
		windowStart = 0f
		windowEnd = 1f
		isAnimating = false
	}

	override fun doUpdate(delta: Float): Boolean
	{
		var complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		if (animTime < windowEndAnimDuration)
		{
			animTime += delta
			val alpha = animTime / windowEndAnimDuration
			windowEnd = min(alpha, 1f)
			windowStart = max(windowEnd - windowSizeLag, 0f)

			complete = false
		}
		isAnimating = animTime < windowEndAnimDuration

		return complete
	}

	override fun doRender(batch: Batch, x: Float, y: Float, tileSize: Float)
	{
		TODO("Not yet implemented")
	}

	fun computeVertices(offsetx: Float, offsety: Float, tileSize: Float, colour: Colour)
	{
		if (this.offsetx == offsetx && this.offsety == offsety && this.tileSize == tileSize && lastCol == colour && windowStart == lastWindowStart && windowEnd == lastWindowEnd)
		{
			return
		}

		this.offsetx = offsetx
		this.offsety = offsety
		this.tileSize = tileSize
		this.lastCol.set(colour)
		this.lastWindowStart = windowStart
		this.lastWindowEnd = windowEnd

		val colBits = lastCol.toFloatBits()
		val step = 1f / (samples - 1)

		val thickness = (thicknessPixels / tileSize) * 0.5f

		var vertI = 0
		for (i in 0 until samples)
		{
			val alpha = windowStart + step * i * (windowEnd - windowStart)
			path.valueAt(sample, alpha)

			if (i == 0)
			{
				path.valueAt(lastSample, step)
				dir.set(lastSample).sub(sample).nor()
			}
			else
			{
				dir.set(sample).sub(lastSample).nor()
			}
			lastSample.set(sample)

			val perp = dir.set(-dir.y, dir.x)
			perp.scl(thickness).scl(tileSize)
			sample.scl(tileSize).add(offsetx, offsety)

			val u = texture.u.lerp(texture.u2, alpha)

			val top = realPos.set(sample).add(perp)
			vertices[vertI++] = top.x
			vertices[vertI++] = top.y
			vertices[vertI++] = colBits
			vertices[vertI++] = u
			vertices[vertI++] = texture.v2

			val bottom = realPos.set(sample).sub(perp)
			vertices[vertI++] = bottom.x
			vertices[vertI++] = bottom.y
			vertices[vertI++] = colBits
			vertices[vertI++] = u
			vertices[vertI++] = texture.v
		}

		var indexI = 0
		for (i in 0 until samples-1)
		{
			indices[indexI++] = (0 + i*2).toShort()
			indices[indexI++] = (1 + i*2).toShort()
			indices[indexI++] = (2 + i*2).toShort()
			indices[indexI++] = (2 + i*2).toShort()
			indices[indexI++] = (1 + i*2).toShort()
			indices[indexI++] = (3 + i*2).toShort()
		}
	}

	override fun copy(): Renderable
	{
		return CurveRenderable(path, thicknessPixels, texture, samples)
	}
}