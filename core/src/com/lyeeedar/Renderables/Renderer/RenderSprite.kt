package com.lyeeedar.Renderables.Renderer

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.BlendMode
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.Colour

// ----------------------------------------------------------------------
class RenderSprite(val parentBlock: RenderSpriteBlock, val parentBlockIndex: Int) : Comparable<RenderSprite>
{
	internal var px: Int = 0
	internal var py: Int = 0
	internal val colour: Colour = Colour(1f, 1f, 1f, 1f)
	internal var sprite: Sprite? = null
	internal var tilingSprite: TilingSprite? = null
	internal var texture: TextureRegion? = null
	internal var nextTexture: TextureRegion? = null
	internal var blendAlpha = 0f
	internal var x: Float = 0f
	internal var y: Float = 0f
	internal var width: Float = 1f
	internal var height: Float = 1f
	internal var rotation: Float = 0f
	internal var scaleX: Float = 1f
	internal var scaleY: Float = 1f
	internal var flipX: Boolean = false
	internal var flipY: Boolean = false
	internal var blend: BlendMode = BlendMode.MULTIPLICATIVE
	internal var isLit: Boolean = true
	internal var alphaRef: Float = 0f

	val tempColour = Colour()
	val tlCol = Colour()
	val trCol = Colour()
	val blCol = Colour()
	val brCol = Colour()

	internal var comparisonVal: Int = 0

	// ----------------------------------------------------------------------
	operator fun set(sprite: Sprite?, tilingSprite: TilingSprite?, texture: TextureRegion?,
	                 x: Float, y: Float,
	                 ix: Float, iy: Float,
	                 colour: Colour,
	                 width: Float, height: Float,
	                 rotation: Float,
	                 scaleX: Float, scaleY: Float,
	                 flipX: Boolean, flipY: Boolean,
	                 blend: BlendMode, lit: Boolean,
	                 comparisonVal: Int): RenderSprite
	{
		this.px = ix.toInt()
		this.py = iy.toInt()
		this.colour.set(colour)
		this.sprite = sprite
		this.tilingSprite = tilingSprite
		this.texture = texture
		this.x = x
		this.y = y
		this.width = width
		this.height = height
		this.comparisonVal = comparisonVal
		this.blend = blend
		this.rotation = rotation
		this.scaleX = scaleX
		this.scaleY = scaleY
		this.flipX = flipX
		this.flipY = flipY
		this.isLit = lit
		this.blendAlpha = 0f
		this.alphaRef = 0f

		nextTexture = null

		return this
	}

	// ----------------------------------------------------------------------
	override fun compareTo(other: RenderSprite): Int
	{
		return comparisonVal.compareTo(other.comparisonVal)
	}

	// ----------------------------------------------------------------------
	internal fun free() = parentBlock.free(this)

	// ----------------------------------------------------------------------
	companion object
	{
		@JvmField var currentBlock: RenderSpriteBlock = RenderSpriteBlock.obtain()

		internal fun obtain(): RenderSprite
		{
			val rs = currentBlock.obtain()

			if (currentBlock.full())
			{
				currentBlock = RenderSpriteBlock.obtain()
			}

			return rs
		}
	}
}

// ----------------------------------------------------------------------
class RenderSpriteBlock
{
	@JvmField var count = 0
	@JvmField var index: Int = 0
	@JvmField val sprites = Array(blockSize) { RenderSprite(this, it) }

	internal inline fun full() = index == blockSize

	internal fun obtain(): RenderSprite
	{
		val sprite = sprites[index]
		index++
		count++

		return sprite
	}

	internal fun free(data: RenderSprite)
	{
		count--

		if (count == 0 && index == blockSize)
		{
			pool.free(this)
			index = 0
		}
	}

	companion object
	{
		const val blockSize: Int = 128

		fun obtain(): RenderSpriteBlock
		{
			val block = pool.obtain()

			return block
		}

		private val pool: Pool<RenderSpriteBlock> = object : Pool<RenderSpriteBlock>() {
			override fun newObject(): RenderSpriteBlock
			{
				return RenderSpriteBlock()
			}
		}
	}
}