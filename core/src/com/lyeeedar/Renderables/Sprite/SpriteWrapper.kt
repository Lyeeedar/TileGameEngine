package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.collections.set
import squidpony.squidmath.LightRNG

/**
 * Created by Philip on 06-Jul-16.
 */

class SpriteWrapper
{
	val rng = LightRNG()

	var sprite: Sprite? = null
	var tilingSprite: TilingSprite? = null

	val spriteVariants = com.badlogic.gdx.utils.Array<Pair<Float, Sprite>>(1)
	val tilingSpriteVariants = com.badlogic.gdx.utils.Array<Pair<Float, TilingSprite>>(1)

	fun getChosenSprite(x: Int, y: Int): Sprite?
	{
		if (spriteVariants.size == 0) return sprite

		val seed = 1000 * x + y
		rng.setSeed(seed.toLong())
		val value = rng.nextFloat()

		var counter = 0f
		for (i in 0 until spriteVariants.size)
		{
			val variant = spriteVariants[i]
			counter += variant.first

			if (value <= counter)
			{
				return variant.second
			}
		}

		return sprite
	}

	fun getChosenTilingSprite(x: Int, y: Int): TilingSprite?
	{
		if (tilingSpriteVariants.size == 0) return tilingSprite

		val seed = 1000 * x + y
		rng.setSeed(seed.toLong())
		val value = rng.nextFloat()

		var counter = 0f
		for (i in 0 until tilingSpriteVariants.size)
		{
			val variant = tilingSpriteVariants[i]
			counter += variant.first

			if (value <= counter)
			{
				return variant.second
			}
		}

		return tilingSprite
	}

	fun copy(): SpriteWrapper
	{
		return this
	}

	companion object
	{
		val loaded = ObjectMap<XmlData, SpriteWrapper>()
		fun load(xml: XmlData): SpriteWrapper
		{
			val existing = loaded[xml]
			if (existing != null) return existing

			var spriteEl = xml.getChildByName("Sprite")
			var tilingEl = xml.getChildByName("TilingSprite")

			if (spriteEl == null && tilingEl == null)
			{
				if (xml.name == "Sprite") spriteEl = xml
				if (xml.name == "TilingSprite") tilingEl = xml
			}

			val wrapper = SpriteWrapper()
			if (spriteEl != null)
			{
				val refKey = spriteEl.getAttribute("meta:RefKey")
				wrapper.sprite = when (refKey)
				{
					"Sprite" -> AssetManager.loadSprite(spriteEl)
					"RenderedLayeredSprite" -> AssetManager.loadLayeredSprite(spriteEl)
					else -> throw RuntimeException("Unhandled spriteVariant refKey '$refKey'")
				}
			}
			if (tilingEl != null) wrapper.tilingSprite = AssetManager.loadTilingSprite(tilingEl)

			val spriteVariantsEl = xml.getChildByName("SpriteVariants")
			if (spriteVariantsEl != null)
			{
				for (el in spriteVariantsEl.children)
				{
					val spriteVariantEl = el.getChildByName("Sprite")!!
					val refKey = spriteVariantEl.getAttribute("meta:RefKey")
					val sprite = when (refKey)
					{
						"Sprite" -> AssetManager.loadSprite(spriteVariantEl)
						"RenderedLayeredSprite" -> AssetManager.loadLayeredSprite(spriteVariantEl)
						else -> throw RuntimeException("Unhandled spriteVariant refKey '$refKey'")
					}

					val weight = el.getFloat("Chance")

					wrapper.spriteVariants.add(Pair(weight, sprite))
				}
			}

			val tilingSpriteVariantsEl = xml.getChildByName("TilingSpriteVariants")
			if (tilingSpriteVariantsEl != null)
			{
				for (el in tilingSpriteVariantsEl.children)
				{
					val sprite = AssetManager.loadTilingSprite(el.getChildByName("TilingSprite")!!)
					val weight = el.getFloat("Chance")

					wrapper.tilingSpriteVariants.add(Pair(weight, sprite))
				}
			}

			loaded[xml] = wrapper

			return wrapper
		}
	}
}