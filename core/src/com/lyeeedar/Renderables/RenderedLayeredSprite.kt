package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.Actions.AbstractActionSequenceAction
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import java.awt.image.BufferedImage
import java.util.*

@DataClass(global = true)
class RenderedLayeredSprite : XmlDataClass()
{
	val layers: Array<ImageLayer> = Array()

	override fun toString(): String
	{
		return layers.joinToString("_") { it.toString() }
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val layersEl = xmlData.getChildByName("Layers")
		if (layersEl != null)
		{
			for (el in layersEl.children)
			{
				val objlayers: ImageLayer
				val objlayersEl = el
				objlayers = ImageLayer()
				objlayers.load(objlayersEl)
				layers.add(objlayers)
			}
		}
	}
	//endregion
}

class ImageLayer : XmlDataClass()
{
	@DataFileReference(basePath = "Sprites", stripExtension = true, allowedFileTypes = "png")
	var path: String = "white"
	var drawActualSize: Boolean = false
	var clip: Boolean = true

	@DataNumericRange(min = 0f)
	var scale: Float = 1f

	val modifiers: Array<AbstractImageModifier> = Array()

	@Transient
	lateinit var pixmap: Pixmap

	override fun toString(): String
	{
		return path + drawActualSize.toString() + clip.toString() + modifiers.joinToString { it.toString() } + scale.toString()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		path = xmlData.get("Path", "white")!!
		drawActualSize = xmlData.getBoolean("DrawActualSize", false)
		clip = xmlData.getBoolean("Clip", true)
		scale = xmlData.getFloat("Scale", 1f)
		val modifiersEl = xmlData.getChildByName("Modifiers")
		if (modifiersEl != null)
		{
			for (el in modifiersEl.children)
			{
				val objmodifiers: AbstractImageModifier
				val objmodifiersEl = el
				objmodifiers = XmlDataClassLoader.loadAbstractImageModifier(objmodifiersEl.get("classID", objmodifiersEl.name)!!)
				objmodifiers.load(objmodifiersEl)
				modifiers.add(objmodifiers)
			}
		}
	}
	//endregion
}

abstract class AbstractImageModifier : XmlDataClass()
{
	abstract fun apply(image: BufferedImage)
	abstract fun asString(): String

	override fun toString(): String
	{
		return javaClass.simpleName + asString()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	//endregion
}

class TintModifier : AbstractImageModifier()
{
	lateinit var colour: Colour

	override fun apply(image: BufferedImage)
	{
		return ImageUtils.tint(image, colour.color())
	}

	override fun asString(): String
	{
		return colour.color().toString()
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
	}
	override val classID: String = "Tint"
	//endregion
}

class StrokeModifier : AbstractImageModifier()
{
	lateinit var colour: Colour
	var thickness: Int = 1

	override fun apply(image: BufferedImage)
	{
		return ImageUtils.stroke(image, thickness, colour.color())
	}

	override fun asString(): String
	{
		return colour.color().toString() + thickness
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
		thickness = xmlData.getInt("Thickness", 1)
	}
	override val classID: String = "Stroke"
	//endregion
}

class GrayscaleModifier : AbstractImageModifier()
{
	override fun apply(image: BufferedImage)
	{
		return ImageUtils.grayscale(image)
	}

	override fun asString(): String
	{
		return ""
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "Grayscale"
	//endregion
}

class GradientModifier : AbstractImageModifier()
{
	enum class Type
	{
		INTERNAL,
		RADIAL,
		VERTICAL,
		HORIZONTAL
	}

	var type: Type = Type.INTERNAL

	@DataTimeline
	val colours: Array<GradientColour> = Array()

	override fun apply(image: BufferedImage)
	{
		return when (type)
		{
			Type.RADIAL -> ImageUtils.radialGradient(image, colours)
			else -> ImageUtils.internalGradient(image, colours)
		}
	}

	override fun asString(): String
	{
		return colours.joinToString { it.toString() }
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		type = Type.valueOf(xmlData.get("Type", Type.INTERNAL.toString())!!.toUpperCase(Locale.ENGLISH))
		val coloursEl = xmlData.getChildByName("Colours")
		if (coloursEl != null)
		{
			for (el in coloursEl.children)
			{
				val objcolours: GradientColour
				val objcoloursEl = el
				objcolours = GradientColour()
				objcolours.load(objcoloursEl)
				colours.add(objcolours)
			}
		}
	}
	override val classID: String = "Gradient"
	//endregion
}

class GradientColour : XmlDataClass()
{
	@DataNumericRange(min = 0f, max = 1f)
	var time: Float = 0f

	lateinit var colour: Colour

	override fun toString(): String
	{
		return "$time${colour.color().toString()}"
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		time = xmlData.getFloat("Time", 0f)
		colour = AssetManager.loadColour(xmlData.getChildByName("Colour")!!)
	}
	//endregion
}