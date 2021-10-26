package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable

open class TextureDrawable : BaseDrawable, TransformDrawable
{
	var texture: Texture? = null
	var color: Color = Color.WHITE.cpy()
	var scale = 1f
	var offsetX = 0f
	var offsetY = 0f

	/** Creates an uninitialized TextureRegionDrawable. The texture region must be set before use.  */
	constructor()
	{
	}

	constructor(texture: Texture)
	{
		this.texture = texture
	}

	constructor(drawable: TextureDrawable) : super(drawable)
	{
		texture = drawable.texture
	}

	override fun draw(batch: Batch?, x: Float, y: Float, width: Float, height: Float)
	{
		batch!!.color = color

		val tex = texture!!

		val u2 = if (tex.uWrap == Texture.TextureWrap.ClampToEdge) 1f else (width / tex.width) * scale
		val v2 = if (tex.vWrap == Texture.TextureWrap.ClampToEdge) 1f else (height / tex.height) * scale
		batch.draw(this.texture, x, y, width, height, offsetX, offsetY, offsetX+u2, offsetY+v2)
	}

	override fun draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float)
	{
		throw NotImplementedError()
	}

	fun scale(scale: Float): TextureDrawable
	{
		this.scale = scale
		return this
	}

	fun offset(offsetX: Float, offsetY: Float): TextureDrawable
	{
		this.offsetX = offsetX
		this.offsetY = offsetY
		return this
	}

	fun tint(tint: Color): TextureDrawable
	{
		color.set(tint)
		return this
	}
}