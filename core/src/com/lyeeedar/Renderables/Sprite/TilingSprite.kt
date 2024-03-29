package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.EnumBitflag
import com.lyeeedar.Util.XmlData
import java.util.*

// Naming priority: NSEW
class TilingSprite() : Renderable()
{
	constructor(name: String, texture: String, mask: String) : this()
	{
		val spriteBase = XmlData("Sprite", "")

		load(name, name, texture, mask, spriteBase)
	}

	var sprites = IntMap<Sprite>()

	var thisID: Long = 0
	var checkID: Long = 0
	var texName: String? = null
	lateinit var maskName: String
	var spriteBase = XmlData("Sprite", "")
	var additive = false

	var hasAllElements: Boolean = false

	override fun copy(): TilingSprite
	{
		val copy = TilingSprite()
		copy.checkID = checkID
		copy.thisID = thisID
		copy.texName = texName
		copy.maskName = maskName
		copy.spriteBase = spriteBase
		copy.hasAllElements = hasAllElements

		for (pair in sprites.entries())
		{
			copy.sprites.put(pair.key, pair.value.copy())
		}

		return copy
	}

	fun parse(xml:XmlData)
	{
		var checkName: String? = xml.get("Name", null)
		var thisName: String? = xml.get("Name", null)

		checkName = xml.get("CheckName", checkName)
		thisName = xml.get("ThisName", thisName)

		val topElement = xml.getChildByName("Top")
		if (topElement != null)
		{
			val frontElement = xml.getChildByName("Front")!!
			val topSprite = AssetManager.loadSprite(topElement)
			val frontSprite = AssetManager.loadSprite(frontElement)

			sprites.put(CENTER, topSprite)
			sprites.put(SOUTH, frontSprite)

			val overhangElement = xml.getChildByName("Overhang")
			if (overhangElement != null)
			{
				val composedTopName = topElement.get("Name") + "_Overhang_" + overhangElement.get("Name")
				val overhangTopSprite = AssetManager.loadSprite(composedTopName)
				overhangTopSprite.drawActualSize = true
				overhangTopSprite.referenceSize = 48f
				sprites.put(NORTH, overhangTopSprite)

				val composedFrontName = frontElement.get("Name") + "_Overhang_" + overhangElement.get("Name")
				val overhangFrontSprite = AssetManager.loadSprite(composedFrontName)
				overhangFrontSprite.drawActualSize = true
				overhangFrontSprite.referenceSize = 48f
				sprites.put(NORTHSOUTH, overhangFrontSprite)
			}

			hasAllElements = true
		}

		val directionsEl = xml.getChildByName("Directions")
		if (directionsEl != null)
		{
			for (el in directionsEl.children())
			{
				val dir = Direction.valueOf(el.name.uppercase(Locale.ENGLISH))
				val sprite = AssetManager.loadSprite(el)

				val bitflag = when (dir)
				{
					Direction.CENTER -> CENTER
					Direction.NORTH -> NORTH
					Direction.SOUTH -> SOUTH
					Direction.EAST -> EAST
					Direction.WEST -> WEST
					Direction.NORTHEAST -> NORTHEAST
					Direction.NORTHWEST -> NORTHWEST
					Direction.SOUTHEAST -> SOUTHEAST
					Direction.SOUTHWEST -> SOUTHWEST
					else -> CENTER
				}

				sprites.put(bitflag, sprite)
			}

			hasAllElements = true
		}

		val spriteElement = xml.getChildByName("Sprite") ?: this.spriteBase
		val texName = spriteElement.get("Name", null)
		val maskName = xml.get("Mask", "")

		this.additive = xml.getBoolean("Additive", false)

		load(thisName!!, checkName!!, texName, maskName!!, spriteElement)
	}

	fun load(thisName: String, checkName: String, texName: String?, maskName: String, spriteElement:XmlData)
	{
		this.thisID = thisName.lowercase(Locale.ENGLISH).hashCode().toLong()
		this.checkID = checkName.lowercase(Locale.ENGLISH).hashCode().toLong()
		this.texName = texName
		this.maskName = maskName
		this.spriteBase = spriteElement
	}

	fun getSprite(emptyDirections: EnumBitflag<Direction>): Sprite
	{
		if (hasAllElements)
		{
			if (emptyDirections.bitFlag == 0)
			{
				return sprites.get(CENTER)
			}
			else if (emptyDirections.contains(Direction.NORTH) && emptyDirections.contains(Direction.SOUTH) && sprites.containsKey(NORTHSOUTH))
			{
				return sprites.get(NORTHSOUTH)
			}
			else if (emptyDirections.contains(Direction.NORTH) && emptyDirections.contains(Direction.EAST) && sprites.containsKey(NORTHEAST))
			{
				return sprites.get(NORTHEAST)
			}
			else if (emptyDirections.contains(Direction.NORTH) && emptyDirections.contains(Direction.WEST) && sprites.containsKey(NORTHWEST))
			{
				return sprites.get(NORTHWEST)
			}
			else if (emptyDirections.contains(Direction.SOUTH) && emptyDirections.contains(Direction.EAST) && sprites.containsKey(SOUTHEAST))
			{
				return sprites.get(SOUTHEAST)
			}
			else if (emptyDirections.contains(Direction.SOUTH) && emptyDirections.contains(Direction.WEST) && sprites.containsKey(SOUTHWEST))
			{
				return sprites.get(SOUTHWEST)
			}
			else if (emptyDirections.contains(Direction.SOUTH) && sprites.containsKey(SOUTH))
			{
				return sprites.get(SOUTH)
			}
			else if (emptyDirections.contains(Direction.NORTH) && sprites.containsKey(NORTH))
			{
				return sprites.get(NORTH)
			}
			else if (emptyDirections.contains(Direction.WEST) && sprites.containsKey(WEST))
			{
				return sprites.get(WEST)
			}
			else if (emptyDirections.contains(Direction.EAST) && sprites.containsKey(EAST))
			{
				return sprites.get(EAST)
			}
			else
			{
				return sprites.get(CENTER)
			}
		}
		else
		{
			var sprite: Sprite? = sprites.get(emptyDirections.bitFlag)
			if (sprite != null)
			{
				return sprite
			}
			else
			{
				val masks = getMasks(emptyDirections)

				var mask = ""
				for (m in masks)
				{
					mask += "_" + m
				}

				if (texName != null)
				{
					val region = getMaskedSprite(texName!!, maskName, masks, additive)
					sprite = AssetManager.loadSprite(spriteBase, region)
				}
				else
				{
					sprite = sprites.get(CENTER)
				}

				sprites.put(emptyDirections.bitFlag, sprite)
				return sprite!!
			}
		}
	}

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

	}

	companion object
	{
		public val CENTER = 1 shl Direction.CENTER.ordinal + 1
		public val SOUTH = 1 shl Direction.SOUTH.ordinal + 1
		public val NORTH = 1 shl Direction.NORTH.ordinal + 1
		public val EAST = 1 shl Direction.EAST.ordinal + 1
		public val WEST = 1 shl Direction.WEST.ordinal + 1
		public val NORTHSOUTH = 0 or NORTH or SOUTH
		public val NORTHEAST = 0 or NORTH or EAST
		public val NORTHWEST = 0 or NORTH or WEST
		public val SOUTHEAST = 0 or SOUTH or EAST
		public val SOUTHWEST = 0 or SOUTH or WEST

		fun load(xml:XmlData): TilingSprite
		{
			val sprite = TilingSprite()
			sprite.parse(xml)
			return sprite
		}

		private fun getMaskedSprite(baseName: String, maskBaseName: String, masks: Array<String>, additive: Boolean): TextureRegion
		{
			// If no masks then just return the original texture
			if (masks.size == 0)
			{
				return AssetManager.tryLoadTextureRegion("Sprites/$baseName.png")!!
			}

			// Build the mask suffix
			var mask = ""
			for (m in masks)
			{
				mask += "_" + m
			}

			val maskedName = baseName + "_" + maskBaseName + mask + "_" + additive

			val tex = AssetManager.tryLoadTextureRegion("Sprites/$maskedName.png")

			// We have the texture, so return it
			if (tex != null)
			{
				return tex
			}

			throw RuntimeException("No masked sprite packed for file: " + maskedName)
		}

		fun getMasks(emptyDirections: EnumBitflag<Direction>): Array<String>
		{
			val masks = Array<String>(8)

			if (emptyDirections.bitFlag == 0)
			{
				masks.add("C")
			}

			if (emptyDirections.contains(Direction.NORTH))
			{
				if (emptyDirections.contains(Direction.EAST))
				{
					masks.add("NE")
				}

				if (emptyDirections.contains(Direction.WEST))
				{
					masks.add("NW")
				}

				if (!emptyDirections.contains(Direction.EAST) && !emptyDirections.contains(Direction.WEST))
				{
					masks.add("N")
				}
			}

			if (emptyDirections.contains(Direction.SOUTH))
			{
				if (emptyDirections.contains(Direction.EAST))
				{
					masks.add("SE")
				}

				if (emptyDirections.contains(Direction.WEST))
				{
					masks.add("SW")
				}

				if (!emptyDirections.contains(Direction.EAST) && !emptyDirections.contains(Direction.WEST))
				{
					masks.add("S")
				}
			}

			if (emptyDirections.contains(Direction.EAST))
			{
				if (!emptyDirections.contains(Direction.NORTH) && !emptyDirections.contains(Direction.SOUTH))
				{
					masks.add("E")
				}
			}

			if (emptyDirections.contains(Direction.WEST))
			{
				if (!emptyDirections.contains(Direction.NORTH) && !emptyDirections.contains(Direction.SOUTH))
				{
					masks.add("W")
				}
			}

			if (emptyDirections.contains(Direction.NORTHEAST) && !emptyDirections.contains(Direction.NORTH) && !emptyDirections.contains(Direction.EAST))
			{
				masks.add("DNE")
			}

			if (emptyDirections.contains(Direction.NORTHWEST) && !emptyDirections.contains(Direction.NORTH) && !emptyDirections.contains(Direction.WEST))
			{
				masks.add("DNW")
			}

			if (emptyDirections.contains(Direction.SOUTHEAST) && !emptyDirections.contains(Direction.SOUTH) && !emptyDirections.contains(Direction.EAST))
			{
				masks.add("DSE")
			}

			if (emptyDirections.contains(Direction.SOUTHWEST) && !emptyDirections.contains(Direction.SOUTH) && !emptyDirections.contains(Direction.WEST))
			{
				masks.add("DSW")
			}

			return masks
		}
	}
}
