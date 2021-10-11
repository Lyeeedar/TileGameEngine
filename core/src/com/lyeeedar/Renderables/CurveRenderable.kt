package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Path
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.lerp
import com.lyeeedar.Util.set

class CurveRenderable(val path: Path<Vector2>, val thicknessPixels: Float, val texture: TextureRegion, val samples: Int) : Renderable()
{
	private val vertexSize = 5
	val vertices: FloatArray = FloatArray(samples * 2 * vertexSize)
	val indices: ShortArray = ShortArray((samples - 1) * 2 * 3)
	private var offsetx: Float = Float.MAX_VALUE
	private var offsety: Float = Float.MAX_VALUE
	private var tileSize: Float = Float.MAX_VALUE
	private val lastCol: Colour = Colour.TRANSPARENT.copy()

	private val sample = Vector2()
	private val lastSample = Vector2()
	private val dir = Vector2()
	private val realPos = Vector2()

	override fun doUpdate(delta: Float): Boolean
	{
		val complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		return complete
	}

	override fun doRender(batch: Batch, x: Float, y: Float, tileSize: Float)
	{
		TODO("Not yet implemented")
	}

	fun computeVertices(offsetx: Float, offsety: Float, tileSize: Float, colour: Colour)
	{
		if (this.offsetx == offsetx && this.offsety == offsety && this.tileSize == tileSize && lastCol == colour)
		{
			return
		}

		this.offsetx = offsetx
		this.offsety = offsety
		this.tileSize = tileSize
		this.lastCol.set(colour)

		val colBits = lastCol.toFloatBits()
		val step = 1f / (samples - 1)

		val thickness = (thicknessPixels / tileSize) * 0.5f

		var vertI = 0
		for (i in 0 until samples)
		{
			val alpha = step * i
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