package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Animation.AbstractColourAnimation
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Shadow
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics.Companion.spriteTargetResolution
import com.lyeeedar.Util.XmlData

@DataClass(name = "Sprite", global = true)
class SpriteData : XmlDataClass()
{
	@DataFileReference(basePath = "Sprites", allowedFileTypes = "png")
	var name: String = "white"

	var drawActualSize: Boolean = false

	@DataNumericRange(min = 0f)
	var updateRate: Float = 0.5f

	var colour: Colour? = null

	var light: Light? = null
	var shadow: Shadow? = null

	var repeatDelay: Float = 0f
	var blend: Boolean = false
	var disableHDR: Boolean = false
	var randomStart: Boolean = false

	//region generated
	override fun load(xmlData: XmlData)
	{
		name = xmlData.get("Name", "white")!!
		drawActualSize = xmlData.getBoolean("DrawActualSize", false)
		updateRate = xmlData.getFloat("UpdateRate", 0.5f)
		colour = AssetManager.tryLoadColour(xmlData.getChildByName("Colour"))
		val lightEl = xmlData.getChildByName("Light")
		if (lightEl != null)
		{
			light = Light()
			light!!.load(lightEl)
		}
		val shadowEl = xmlData.getChildByName("Shadow")
		if (shadowEl != null)
		{
			shadow = Shadow()
			shadow!!.load(shadowEl)
		}
		repeatDelay = xmlData.getFloat("RepeatDelay", 0f)
		blend = xmlData.getBoolean("Blend", false)
		disableHDR = xmlData.getBoolean("DisableHDR", false)
		randomStart = xmlData.getBoolean("RandomStart", false)
	}
	//endregion
}

class Sprite(val fileName: String, var animationDelay: Float, var textures: Array<TextureRegion>, colour: Colour, var drawActualSize: Boolean) : Renderable()
{
	constructor(image: TextureRegion, colour: Colour = Colour.WHITE, drawActualSize: Boolean = false) : this("", 1f, Array<TextureRegion>(arrayOf(image)), colour, drawActualSize)
	{
	}

	enum class AnimationStage
	{
		INVALID,
		START,
		MIDDLE,
		END;

		companion object
		{
			val Values = values()
		}
	}

	var referenceSize: Float? = null

	var disableHDR: Boolean = false

	var tempCol = Colour()
	var oldCol = Colour()

	var colourAnimation: AbstractColourAnimation? = null

	var repeatDelay = 0f
	var repeatAccumulator: Float = 0.toFloat()
	var animationAccumulator: Float = 0.toFloat()

	var fixPosition: Boolean = false

	var completed = false

	var animationStage = AnimationStage.INVALID

	var baseScale = floatArrayOf(1f, 1f)

	var completionCallback: (() -> Unit)? = null

	var removeAmount: Float = 0.0f

	var frameBlend = false

	var texIndex: Int = 0

	var tintLight = false

	private val tempColour = Colour()
	private val tempVec = Vector3()

	init
	{
		this.colour = colour
	}

	val lifetime: Float
		get() = if (hasAnim) animation!!.duration() else animationDelay * textures.size

	val remainingLifetime: Float
		get() = if (hasAnim) animation!!.duration() - animation!!.time() else animationDelay * (textures.size - texIndex)

	fun randomiseAnimation()
	{
		texIndex = Random.random(Random.sharedRandom, textures.size)
		animationAccumulator = Random.random(Random.sharedRandom, animationDelay)
	}

	override fun doUpdate(delta: Float): Boolean
	{
		var looped = false
		if (textures.size > 1)
		{
			if (repeatAccumulator > 0)
			{
				repeatAccumulator -= delta
			}

			if (repeatAccumulator <= 0)
			{
				if (animationStage == AnimationStage.INVALID) animationStage = AnimationStage.START
				animationAccumulator += delta

				while (animationAccumulator >= animationDelay)
				{
					animationAccumulator -= animationDelay

					if (animation == null && texIndex == textures.size / 2)
					{
						animationStage = AnimationStage.MIDDLE
					}

					texIndex++
					if (texIndex >= textures.size)
					{
						texIndex = 0
						looped = true
						repeatAccumulator = repeatDelay
					}
				}
			}
		}

		if (hasAnim)
		{
			if (animationStage == AnimationStage.INVALID) animationStage = AnimationStage.START
			looped = animation!!.update(delta)

			if (animation!!.time() >= animation!!.duration() / 2f)
			{
				if (animation == null && texIndex == textures.size / 2)
				{
					animationStage = AnimationStage.MIDDLE
				}
			}

			if (looped)
			{
				animation!!.free()
				animation = null
			}
		}

		if (colourAnimation != null)
		{
			val looped = colourAnimation!!.update(delta)
			if (looped && colourAnimation!!.oneTime)
			{
				colourAnimation!!.free()
				colourAnimation = null
			}
		}

		if (looped)
		{
			animationStage = AnimationStage.END

			completionCallback?.invoke()
			completionCallback = null
		}

		if (tintLight && light != null)
		{
			val colour =
				if (colourAnimation != null) colourAnimation!!.renderColour()!!
				else if (animation?.renderColour() != null) animation!!.renderColour()!!
				else this.colour

			light!!.colour.set(light!!.baseColour).mul(colour)
		}

		if (!completed) completed = looped
		return looped
	}

	override fun doRender(batch: Batch, x: Float, y: Float, size: Float)
	{
		var scaleX = baseScale[0]
		var scaleY = baseScale[1]

		if (hasAnim)
		{
			val scale = animation!!.renderScale()
			if (scale != null)
			{
				scaleX *= scale[0]
				scaleY *= scale[1]
			}
		}

		render(batch, x, y, size, size, scaleX, scaleY, rotation)
	}

	fun render(batch: Batch, x: Float, y: Float, width: Float, height: Float, scaleX: Float = 1f, scaleY: Float = 1f, rotation: Float = 0f)
	{
		var scaleX = baseScale[0] * scaleX
		var scaleY = baseScale[1] * scaleY

		if (hasAnim)
		{
			val scale = animation!!.renderScale()
			if (scale != null)
			{
				scaleX *= scale[0]
				scaleY *= scale[1]
			}
		}

		render(batch, x, y, width, height, scaleX, scaleY, texIndex, rotation + this.rotation)
	}

	private fun render(batch: Batch, x: Float, y: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, texIndex: Int, rotation: Float)
	{
		val colour =
			if (colourAnimation != null) colourAnimation!!.renderColour()!!
			else if (animation?.renderColour() != null) animation!!.renderColour()!!
			else this.colour

		if (colour.a == 0f)
		{
			return
		}

		if (colour == Colour.WHITE && !disableHDR)
		{
			drawTexture(batch, texIndex, x, y, width, height, scaleX, scaleY, rotation)
		}
		else
		{
			val c = oldCol.set(batch.color, batch.packedColor)
			val oldCol = oldCol.set(c)

			val col = tempColour.set(oldCol)
			col *= colour

			if (disableHDR)
			{
				col.clamp()
			}

			batch.packedColor = col.toFloatBits()

			drawTexture(batch, texIndex, x, y, width, height, scaleX, scaleY, rotation)

			batch.packedColor = oldCol.toFloatBits()
		}
	}

	private fun drawTexture(batch: Batch, textureIndex: Int, x: Float, y: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float)
	{
		var x = x
		var y = y
		var width = width
		var height = height

		width *= size[0]
		height *= size[1]

		val texture = textures[textureIndex]

		if (drawActualSize)
		{
			val widthRatio = width / spriteTargetResolution
			val heightRatio = height / spriteTargetResolution

			val regionWidth = referenceSize ?: texture.regionWidth.toFloat()
			val regionHeight = referenceSize ?: texture.regionHeight.toFloat()

			val trueWidth = regionWidth * widthRatio
			val trueHeight = regionHeight * heightRatio

			val widthOffset = (trueWidth - width) / 2

			x -= widthOffset
			width = trueWidth
			height = trueHeight
		}

		if (rotation != 0f && fixPosition)
		{
			val offset = getPositionCorrectionOffsets(x, y, width / 2.0f, height / 2.0f, width, height, scaleX, scaleY, rotation, tempVec)
			x -= offset.x
			y -= offset.y
		}

		if (frameBlend && textures.size > 1)
		{
			val alpha = animationAccumulator / animationDelay
			val nextIndex = if (textureIndex+1 == textures.size) 0 else textureIndex+1

			com.lyeeedar.Renderables.drawBlendBatch(batch, texture, textures[nextIndex], alpha, x, y, width / 2.0f, height / 2.0f, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount)
		}
		else
		{
			com.lyeeedar.Renderables.drawBatch(batch, texture, x, y, width / 2.0f, height / 2.0f, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount)
		}
	}

	fun getRenderColour(): Colour
	{
		val renderCol = when
		{
			(colourAnimation != null) -> colourAnimation!!.renderColour()!!
			(animation?.renderColour() != null) -> animation!!.renderColour()!!
			else -> this.colour
		}

		return renderCol
	}

	fun render(vertices: FloatArray, offset: Int, colour: Colour, x: Float, y: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, isLit: Boolean)
	{
		var scaleX = baseScale[0] * scaleX
		var scaleY = baseScale[1] * scaleY

		if (hasAnim)
		{
			val scale = animation!!.renderScale()
			if (scale != null)
			{
				scaleX *= scale[0]
				scaleY *= scale[1]
			}
		}

		val rotation = rotation + this.rotation

		var x = x
		var y = y
		var width = width
		var height = height

		width *= size[0]
		height *= size[1]

		val texture = textures[texIndex]

		if (drawActualSize)
		{
			val widthRatio = width / spriteTargetResolution
			val heightRatio = height / spriteTargetResolution

			val regionWidth = referenceSize ?: texture.regionWidth.toFloat()
			val regionHeight = referenceSize ?: texture.regionHeight.toFloat()

			val trueWidth = regionWidth * widthRatio
			val trueHeight = regionHeight * heightRatio

			val widthOffset = (trueWidth - width) / 2

			x -= widthOffset
			width = trueWidth
			height = trueHeight
		}

		if (rotation != 0f && fixPosition)
		{
			val tempVec = Vector3()
			val offset = getPositionCorrectionOffsets(x, y, width / 2.0f, height / 2.0f, width, height, scaleX, scaleY, rotation, tempVec)
			x -= offset.x
			y -= offset.y
		}

		val tex1: TextureRegion
		val tex2: TextureRegion
		val texAlpha: Float
		if (frameBlend && textures.size > 1)
		{
			val alpha = animationAccumulator / animationDelay
			val nextIndex = if (texIndex+1 == textures.size) 0 else texIndex+1

			tex1 = texture
			tex2 = textures[nextIndex]
			texAlpha = alpha
		}
		else
		{
			tex1 = texture
			tex2 = texture
			texAlpha = 0f
		}

		com.lyeeedar.Renderables.writeInstanceData(vertices, offset, tex1, tex2, colour, x, y, width, height, scaleX, scaleY, rotation, flipX, flipY, removeAmount, texAlpha, 0f, isLit, !drawActualSize)
	}

	inline val currentTexture: TextureRegion
		get() = textures.get(texIndex)

	fun resetAnimation()
	{
		animationAccumulator = 0f
		repeatAccumulator = 0f
		texIndex = 0
	}

	override fun copy(): Sprite
	{
		val sprite = Sprite(fileName, animationDelay, textures, colour, drawActualSize)
		sprite.referenceSize = referenceSize
		sprite.animation = animation?.copy()
		sprite.colourAnimation = colourAnimation?.copy() as? AbstractColourAnimation
		sprite.disableHDR = disableHDR
		sprite.light = light?.copy()
		sprite.shadow = shadow?.copy()
		sprite.colour = colour.copy()
		sprite.tintLight = tintLight

		return sprite
	}

	companion object
	{
		inline fun getPositionCorrectionOffsets(x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float,
												 scaleX: Float, scaleY: Float, rotation: Float, tempVec: Vector3): Vector3
		{
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

			val x1: Float
			val y1: Float
			val x2: Float
			val y2: Float
			val x3: Float
			val y3: Float
			val x4: Float
			val y4: Float

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

			tempVec.set(x1, y1, 0f)

			if (x2 < tempVec.x) tempVec.x = x2
			if (x3 < tempVec.x) tempVec.x = x3
			if (x4 < tempVec.x) tempVec.x = x4

			if (y2 < tempVec.y) tempVec.y = y2
			if (y3 < tempVec.y) tempVec.y = y3
			if (y4 < tempVec.y) tempVec.y = y4

			tempVec.x += worldOriginX
			tempVec.y += worldOriginY

			tempVec.x -= x
			tempVec.y -= y

			return tempVec
		}
	}
}