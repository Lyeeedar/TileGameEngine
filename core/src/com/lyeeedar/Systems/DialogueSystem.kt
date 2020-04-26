package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics

class DialogueSystem(world: World) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.Dialogue).get())
{
	val layout = GlyphLayout()
	val font = Statics.skin.getFont("default")
	val speechBubbleBack = NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/SpeechBubble.png")!!, 10, 10, 10, 10)
	val speechBubbleArrow = AssetManager.loadTextureRegion("Sprites/GUI/SpeechBubbleArrow.png")!!

	val tileSize: Float
		get() = world.tileSize

	val batch: SpriteBatch by lazy { SpriteBatch() }

	val tempCol = Color()

	override fun onTurnEntity(entity: Entity)
	{
		val dialogue = entity.dialogue()!!
		if (dialogue.remainingTurnsToShow > 0)
		{
			dialogue.remainingTurnsToShow--
			if (dialogue.remainingTurnsToShow == 0)
			{
				dialogue.remove = true
			}
		}
	}

	override fun beforeUpdate(deltaTime: Float)
	{
		batch.begin()
	}

	override fun afterUpdate(deltaTime: Float)
	{
		batch.end()
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val dialogue = entity.dialogue()!!

		if (dialogue.remove)
		{
			dialogue.textFade -= deltaTime
			if (dialogue.textFade <= 0f)
			{
				entity.removeComponent(ComponentType.Dialogue)
				return
			}
		}

		if (dialogue.displayedText != dialogue.data.text)
		{
			dialogue.textAccumulator += deltaTime
			while (dialogue.textAccumulator >= 0.02f)
			{
				dialogue.textAccumulator -= 0.02f

				val currentPos = dialogue.displayedText.length
				val nextChar = dialogue.data.text[currentPos]
				var nextString = "" + nextChar
				if (nextChar == '[')
				{
					// this is a colour tag, so read ahead to the closing tag, and the letter after
					var current = currentPos + 1
					while (true)
					{
						val char = dialogue.data.text[current]
						nextString += char

						current++
						if (char == ']') break
					}

					if (current < dialogue.data.text.length)
					{
						val char = dialogue.data.text[current]
						nextString += char
					}
				}

				dialogue.displayedText += nextString

				if (dialogue.displayedText == dialogue.data.text) break
			}
		}

		tempCol.set(1f, 1f, 1f, dialogue.alpha)

		var x = entity.position()!!.x * tileSize
		var y = entity.position()!!.y * tileSize

		val renderOffset = entity.renderOffset()
		if (renderOffset != null)
		{
			x += renderOffset[0] * tileSize
			y += renderOffset[1] * tileSize
		}

		x += tileSize * 0.5f

		if (entity.renderable()?.renderable is Sprite && (entity.renderable()!!.renderable as Sprite).drawActualSize)
		{
			y += tileSize * 1.5f
		}
		else
		{
			y += tileSize
		}

		layout.setText(font, dialogue.data.text, tempCol, Statics.stage.width * 0.5f, Align.left, true)

		var left = x - (layout.width * 0.5f) - 10f
		if (left < 0) left = 0f

		val right = left + layout.width + 20
		if (right >= Statics.stage.width) left = right - Statics.stage.width

		val width = layout.width
		val height = layout.height

		layout.setText(font, dialogue.displayedText, tempCol, Statics.stage.width * 0.5f, Align.left, true)

		batch.color = tempCol
		speechBubbleBack.draw( batch, left, y, width + 20, height + 20 )
		batch.draw( speechBubbleArrow, x - 4f, y - 6f, 8f, 8f )

		font.draw( batch, layout, left + 10, y + layout.height + 10 )
	}
}
