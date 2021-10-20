package com.lyeeedar.Renderables

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import java.util.*
import ktx.math.compareTo
import squidpony.squidgrid.FOV
import squidpony.squidmath.LightRNG

// Could be encoded as
// vec2 pos : 2x4 = 8 bytes
// float packedColour&Brightness : 4 bytes (1 bit per channel)
// float range : 4 bytes
//
// vec4 posx, posy, packedColBrightness, range

class Light(): XmlDataClass(), Comparable<Light>
{
	@DataValue(dataName = "Colour")
	var baseColour: Colour = Colour.WHITE.copy()

	@DataValue(dataName = "Brightness")
	var baseBrightness: Float = 1f

	@DataValue(dataName = "Range")
	var baseRange: Float = 3f
	var hasShadows: Boolean = false
	var anim: LightAnimation? = null

	//region non-data
	var pos = Vector2()

	val colour = Colour.WHITE.copy()
	var brightness: Float = 1f
	var range: Float = 3f

	val cache: ShadowCastCache = ShadowCastCache(fovType = FOV.SHADOW)

	var batchID: Int = 0

	private val pointArray = Array<Point>(false, 16)
	private var last: String = ""
	//endregion

	fun packColourBrightness(): Int
	{
		val byter = (colour.r * 255).toChar()
		val byteg = (colour.g * 255).toChar()
		val byteb = (colour.b * 255).toChar()
		val bytea = (brightness).toChar()

		return packBytesToInt(byter, byteg, byteb, bytea)
	}

	fun update(delta: Float)
	{
		if (hasShadows)
		{
			cache.getShadowCast(pos.x.toInt(), pos.y.toInt(), range.ciel())
		}

		anim?.update(delta, this)
	}

	fun getLightPoints(): Array<Point>
	{
		if (hasShadows)
		{
			return cache.getShadowCast(pos.x.toInt(), pos.y.toInt(), range.ciel())
		}
		else
		{
			val key = "${pos.x}$range"
			if (last == key) return pointArray
			last = key

			val cx = pos.x.toInt()
			val cy = pos.y.toInt()
			val range = range.ciel()

			Point.freeAll(pointArray)
			pointArray.clear()

			for (x in cx - range until cx + range)
			{
				for (y in cy - range until cy + range)
				{
					pointArray.add(Point.obtain().set(x, y))
				}
			}

			return pointArray
		}
	}

	override fun afterLoad()
	{
		colour.set(baseColour)
		range = baseRange
		brightness = baseBrightness
	}

	fun copy(): Light
	{
		val light = Light()
		light.baseColour = baseColour.copy()
		light.baseRange = baseRange
		light.baseBrightness = baseBrightness

		light.anim = anim?.copy()

		light.afterLoad()

		return light
	}

	override fun hashCode(): Int
	{
		val posHash = ((pos.x * 1000 + pos.y) * 1000).toInt()
		val colHash = colour.hashCode()
		val rangeHash = (range * 1000).toInt()

		return posHash + colHash + rangeHash
	}

	override fun compareTo(other: Light): Int
	{
		return pos.compareTo(other.pos)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		baseColour = AssetManager.tryLoadColour(xmlData.getChildByName("Colour"))!!
		baseBrightness = xmlData.getFloat("Brightness", 1f)
		baseRange = xmlData.getFloat("Range", 3f)
		hasShadows = xmlData.getBoolean("HasShadows", false)
		val animEl = xmlData.getChildByName("Anim")
		if (animEl != null)
		{
			anim = XmlDataClassLoader.loadLightAnimation(animEl.get("classID", animEl.name)!!)
			anim!!.load(animEl)
		}
		afterLoad()
	}
	//endregion
}

abstract class LightAnimation : XmlDataClass()
{
	abstract fun update(delta: Float, light: Light)

	abstract fun copy(): LightAnimation

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	//endregion
}

class PulseLightAnimation : LightAnimation()
{
	var periodRange: Vector2 = Vector2()
	var minBrightnessRange: Vector2 = Vector2()
	var maxBrightnessRange: Vector2 = Vector2()
	var minRangeRange: Vector2 = Vector2()
	var maxRangeRange: Vector2 = Vector2()

	//region non-data
	var toMax = true
	var currentPeriod = -1f
	var startBrightness = 0f
	var targetBrightness = 0f
	var startRange = 0f
	var targetRange = 0f
	var time = 0f
	//end-region

	override fun update(delta: Float, light: Light)
	{
		if (currentPeriod < 0f)
		{
			currentPeriod = getValue(periodRange)
			startBrightness = getValue(minBrightnessRange)
			targetBrightness = getValue(maxBrightnessRange)
			startRange = getValue(minRangeRange)
			targetRange = getValue(maxRangeRange)
			time = 0f
		}

		time += delta

		if (time > currentPeriod)
		{
			time -= currentPeriod

			toMax = !toMax

			currentPeriod = getValue(periodRange)
			startBrightness = targetBrightness
			startRange = targetRange

			targetBrightness = if (toMax) getValue(maxBrightnessRange) else getValue(minBrightnessRange)
			targetRange = if (toMax) getValue(maxRangeRange) else getValue(minRangeRange)
		}

		val alpha = time / currentPeriod
		val brightness = startBrightness.lerp(targetBrightness, alpha)
		val range = startRange.lerp(targetRange, alpha)

		light.colour.set(light.baseColour)
		light.brightness = light.baseBrightness * brightness
		light.range = light.baseRange * range
	}

	fun getValue(range: Vector2, ran: LightRNG = Random.sharedRandom): Float
	{
		return range.x + ran.nextFloat() * (range.y - range.x)
	}

	override fun copy(): LightAnimation
	{
		val anim = PulseLightAnimation()
		anim.periodRange = periodRange
		anim.minBrightnessRange = minBrightnessRange
		anim.maxBrightnessRange = maxBrightnessRange
		anim.minRangeRange = minRangeRange
		anim.maxRangeRange = maxRangeRange

		return anim
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val periodRangeRaw = xmlData.get("PeriodRange", "0,0")!!.split(',')
		periodRange = Vector2(periodRangeRaw[0].trim().toFloat(), periodRangeRaw[1].trim().toFloat())
		val minBrightnessRangeRaw = xmlData.get("MinBrightnessRange", "0,0")!!.split(',')
		minBrightnessRange = Vector2(minBrightnessRangeRaw[0].trim().toFloat(), minBrightnessRangeRaw[1].trim().toFloat())
		val maxBrightnessRangeRaw = xmlData.get("MaxBrightnessRange", "0,0")!!.split(',')
		maxBrightnessRange = Vector2(maxBrightnessRangeRaw[0].trim().toFloat(), maxBrightnessRangeRaw[1].trim().toFloat())
		val minRangeRangeRaw = xmlData.get("MinRangeRange", "0,0")!!.split(',')
		minRangeRange = Vector2(minRangeRangeRaw[0].trim().toFloat(), minRangeRangeRaw[1].trim().toFloat())
		val maxRangeRangeRaw = xmlData.get("MaxRangeRange", "0,0")!!.split(',')
		maxRangeRange = Vector2(maxRangeRangeRaw[0].trim().toFloat(), maxRangeRangeRaw[1].trim().toFloat())
	}
	override val classID: String = "Pulse"
	//endregion
}