package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.clamp
import com.lyeeedar.Util.packFloats

internal inline fun drawBatch(batch: Batch, region: TextureRegion,
		 x: Float, y: Float, originX: Float, originY: Float,
		 width: Float, height: Float, scaleX: Float, scaleY: Float,
		 rotation: Float, flipX: Boolean, flipY: Boolean, removeAmount: Float)
{
	doDrawBatch(batch, region, batch.packedColor, x, y, originX, originY, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount)
}

val tempCol1 = Colour()
val tempCol2 = Colour()
internal inline fun drawBlendBatch(batch: Batch, region1: TextureRegion, region2: TextureRegion, blendAlpha: Float,
				x: Float, y: Float, originX: Float, originY: Float,
				width: Float, height: Float, scaleX: Float, scaleY: Float,
				rotation: Float, flipX: Boolean, flipY: Boolean, removeAmount: Float)
{
	val blendAlpha = blendAlpha.clamp(0f, 1f)

	tempCol1.set(batch.color, batch.packedColor)

	tempCol2.set(tempCol1)
	tempCol2.a *= 1f - blendAlpha

	doDrawBatch(batch, region1, tempCol2.toFloatBits(), x, y, originX, originY, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount)

	tempCol2.set(tempCol1)
	tempCol2.a *= blendAlpha

	doDrawBatch(batch, region2, tempCol2.toFloatBits(), x, y, originX, originY, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount)
}

// 4 vertices of order x, y, colour, u, v
val verticesSpriteBatch: FloatArray by lazy { FloatArray(4 * 5) }
internal inline fun doDrawBatch(batch: Batch, region: TextureRegion, packedColor: Float,
		   x: Float, y: Float, originX: Float, originY: Float,
		   width: Float, height: Float, scaleX: Float, scaleY: Float,
		   rotation: Float, flipX: Boolean, flipY: Boolean, removeAmount: Float)
{
	//##################################################################### Vertex Calculation #######################################//
	// bottom left and top right corner points relative to origin
	val worldOriginX = x + originX
	val worldOriginY = y + originY
	var fx = -originX
	var fy = -originY
	var fx2 = width - originX
	var fy2 = height - originY

	// scale
	if (scaleX != 1f || scaleY != 1f)
	{
		fx *= scaleX
		fy *= scaleY
		fx2 *= scaleX
		fy2 *= scaleY
	}

	// construct corner points, start from top left and go counter clockwise
	val p1x = fx
	val p1y = fy
	val p2x = fx
	val p2y = fy2
	val p3x = fx2
	val p3y = fy2
	val p4x = fx2
	val p4y = fy

	var x1: Float
	var y1: Float
	var x2: Float
	var y2: Float
	var x3: Float
	var y3: Float
	var x4: Float
	var y4: Float

	// rotate
	if (rotation != 0f)
	{
		val cos = MathUtils.cosDeg(rotation)
		val sin = MathUtils.sinDeg(rotation)

		x1 = cos * p1x - sin * p1y
		y1 = sin * p1x + cos * p1y

		x2 = cos * p2x - sin * p2y
		y2 = sin * p2x + cos * p2y

		x3 = cos * p3x - sin * p3y
		y3 = sin * p3x + cos * p3y

		x4 = x1 + (x3 - x2)
		y4 = y3 - (y2 - y1)
	}
	else
	{
		x1 = p1x
		y1 = p1y

		x2 = p2x
		y2 = p2y

		x3 = p3x
		y3 = p3y

		x4 = p4x
		y4 = p4y
	}

	x1 += worldOriginX
	y1 += worldOriginY
	x2 += worldOriginX
	y2 += worldOriginY
	x3 += worldOriginX
	y3 += worldOriginY
	x4 += worldOriginX
	y4 += worldOriginY

	val r1u: Float
	var r1v: Float
	val r1u2: Float
	val r1v2: Float

	val r2u: Float
	var r2v: Float
	val r2u2: Float
	val r2v2 : Float

	if (flipX)
	{
		r1u = region.u2
		r1u2 = region.u
		r2u = r1u
		r2u2 = r1u2
	}
	else
	{
		r1u = region.u
		r1u2 = region.u2
		r2u = r1u
		r2u2 = r1u2
	}

	if (flipY)
	{
		r1v = region.v
		r1v2 = region.v2
		r2v = r1v
		r2v2 = r1v2
	}
	else
	{
		r1v = region.v2
		r1v2 = region.v
		r2v = r1v
		r2v2 = r1v2
	}

	if (removeAmount > 0f)
	{
		val yMove = (y1-y2) * removeAmount
		y1 -= yMove / 2f
		y4 -= yMove / 2f

		y2 += yMove / 2f
		y3 += yMove / 2f

		val vMove1 = (r1v-r1v2) * removeAmount
		r1v -= vMove1

		val vMove2 = (r2v-r2v2) * removeAmount
		r2v -= vMove2
	}
	//##################################################################### Vertex Calculation #######################################//

	val vertices = verticesSpriteBatch
	vertices[0] = x1
	vertices[1] = y1
	vertices[2] = packedColor
	vertices[3] = r1u
	vertices[4] = r1v

	vertices[5] = x2
	vertices[6] = y2
	vertices[7] = packedColor
	vertices[8] = r1u
	vertices[9] = r1v2

	vertices[10] = x3
	vertices[11] = y3
	vertices[12] = packedColor
	vertices[13] = r1u2
	vertices[14] = r1v2

	vertices[15] = x4
	vertices[16] = y4
	vertices[17] = packedColor
	vertices[18] = r1u2
	vertices[19] = r1v

	batch.draw(region.texture, vertices, 0, 20)
}

val defaultData = packFloats(0f, 0f, 0f, 0f)
internal inline fun writeInstanceData(instanceData: FloatArray, offset: Int, region1: TextureRegion, region2: TextureRegion, colour: Colour,
						   x: Float, y: Float, width: Float, height: Float, scaleX: Float, scaleY: Float,
						   rotation: Float, flipX: Boolean, flipY: Boolean, removeAmount: Float, blendAlpha: Float, alphaRef: Float, isLit: Boolean, smoothShade: Boolean)
{
	var worldX = x + width / 2f
	var worldY = y + height / 2f

	var worldWidth = width * scaleX
	var worldHeight = height * scaleY

	val r1u: Float
	var r1v: Float
	val r1u2: Float
	val r1v2: Float

	val r2u: Float
	var r2v: Float
	val r2u2: Float
	val r2v2 : Float

	if (flipX)
	{
		r1u = region1.u2
		r1u2 = region1.u
		r2u = region2.u2
		r2u2 = region2.u
	}
	else
	{
		r1u = region1.u
		r1u2 = region1.u2
		r2u = region2.u
		r2u2 = region2.u2
	}

	if (flipY)
	{
		r1v = region1.v
		r1v2 = region1.v2
		r2v = region2.v
		r2v2 = region2.v2
	}
	else
	{
		r1v = region1.v2
		r1v2 = region1.v
		r2v = region2.v2
		r2v2 = region2.v
	}

	if (removeAmount > 0f)
	{
		val yMove = worldHeight * removeAmount
		worldHeight -= yMove

		val vMove1 = (r1v-r1v2) * removeAmount
		r1v -= vMove1

		val vMove2 = (r2v-r2v2) * removeAmount
		r2v -= vMove2
	}

	val scaledRotation = (MathUtils.degreesToRadians * rotation) / (MathUtils.PI2)

	val packedCol = colour.toFloatBits()
	val packedData = if (blendAlpha == 0f && isLit && alphaRef == 0f && scaledRotation == 0f)
	{
		defaultData
	}
	else
	{
		packFloats(blendAlpha, if (isLit) 0.0f else 1.0f, alphaRef, scaledRotation)
	}

	var i = offset
	instanceData[i++] = worldX
	instanceData[i++] = worldY
	instanceData[i++] = worldWidth
	instanceData[i++] = worldHeight
	instanceData[i++] = r1u
	instanceData[i++] = r1v
	instanceData[i++] = r1u2
	instanceData[i++] = r1v2
	instanceData[i++] = r2u
	instanceData[i++] = r2v
	instanceData[i++] = r2u2
	instanceData[i++] = r2v2
	instanceData[i++] = packedCol
	instanceData[i++] = packedData
}
