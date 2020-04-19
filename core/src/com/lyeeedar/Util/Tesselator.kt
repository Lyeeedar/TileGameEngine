package com.lyeeedar.Util

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2

class Tesselator
{
	companion object
	{
		fun shatter(textureRegion: TextureRegion): Array<TextureRegion>
		{
			val u = textureRegion.u
			val u2 = textureRegion.u2
			val v = textureRegion.v
			val v2 = textureRegion.v2

			val asQuad = Quad(Vector2(u, v), Vector2(u, v2), Vector2(u2, v2), Vector2(u2, v))
			val subdivisions: Iterable<Quad> = subdivideQuad(asQuad).flatMap { subdivideQuad(it).asIterable() }

			return subdivisions.map {
				TextureRegion(textureRegion.texture, it.u, it.v, it.u2, it.v2)
			}.toTypedArray()
		}

		fun subdivideQuad(quad: Quad): Array<Quad>
		{
			return generateSubdivisionQuads().map {
				Quad(quad.blerp(it.bl), quad.blerp(it.tl), quad.blerp(it.tr), quad.blerp(it.br))
			}.toTypedArray()
		}

		private fun generateSubdivisionQuads(): Array<Quad>
		{
			//--------------------
			// quad points
			// tl               tr
			//
			//         t
			//
			// l                 r
			//
			//         b
			//
			// bl                br
			//--------------------

			val pointMin = 0.05f
			val pointMax = 0.95f

			val lY = Random.random(pointMin, pointMax)
			val rY = Random.random(pointMin, pointMax)

			val bX = Random.random(pointMin, pointMax)
			val tX = Random.random(pointMin, pointMax)
			val bY = Random.random(pointMin, 0.7f)
			val tY = Random.random(bY, pointMax)

			val bl = Vector2(0f, 0f)
			val br = Vector2(0f, 0f)
			val l = Vector2(0f, lY)
			val r = Vector2(1f, rY)
			val b = Vector2(bX, bY)
			val t = Vector2(tX, tY)
			val tl = Vector2(0f, 1f)
			val tr = Vector2(1f, 1f)

			return arrayOf(
				Quad(bl, l, b, br),
				Quad(l, tl, t, b),
				Quad(b, t, r, br),
				Quad(t, tl, tr, r))
		}
	}
}

class Quad(val bl: Vector2, val tl: Vector2, val tr: Vector2, val br: Vector2)
{
	val u: Float
		get() = bl.x

	val u2: Float
		get() = tr.x

	val v: Float
		get() = bl.y

	val v2: Float
		get() = tr.y


	private val temp1 = Vector2()
	private val temp2 = Vector2()

	fun blerp(point: Vector2): Vector2
	{
		val l = temp1.set(bl).lerp(tl, point.y)
		val r = temp2.set(br).lerp(tr, point.y)

		return Vector2().set(l).lerp(r, point.x)
	}
}