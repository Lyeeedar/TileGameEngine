package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin

class ScrollingTextLabel(text: String, val skin: Skin, style: String = "default") : Label(text, skin, style)
{
	init
	{
		setWrap(true)
	}

	override fun setText(newText: CharSequence?)
	{
		super.setText(newText)
		currentTextIndex = 0
		textAccumulator = 0f
	}

	private val maxTextIndex: Int
		get() {
			var accumulator = 0
			for (l in 0 until bitmapFontCache.layouts.size)
			{
				val layout = bitmapFontCache.layouts[l]
				for (r in 0 until layout.runs.size)
				{
					val run = layout.runs[r]
					accumulator += run.glyphs.size
				}
			}

			return accumulator
		}

	private var currentTextIndex: Int = 0
	private var textAccumulator: Float = 0f

	var isComplete: Boolean
		get() = currentTextIndex == maxTextIndex
		set(value)
		{
			if (value)
			{
				currentTextIndex = maxTextIndex
			}
		}

	override fun act(delta: Float)
	{
		val maxIndex = maxTextIndex
		if (currentTextIndex != maxIndex)
		{
			textAccumulator += delta
			while (textAccumulator > 0.02f)
			{
				textAccumulator -= 0.02f
				currentTextIndex++
				if (currentTextIndex > maxIndex)
				{
					currentTextIndex = maxIndex
					break
				}
			}
		}

		super.act(delta)
	}

	private val tempColour: Color = Color()
	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (currentTextIndex == 0) return

		validate()
		val color = tempColour.set(color)
		color.a *= parentAlpha
		if (style.background != null)
		{
			batch!!.setColor(color.r, color.g, color.b, color.a)
			style.background.draw(batch, x, y, width, height)
		}
		if (style.fontColor != null) color.mul(style.fontColor)
		val cache = bitmapFontCache
		cache.tint(color)
		cache.setPosition(x, y)
		cache.draw(batch, 0, currentTextIndex)
	}
}