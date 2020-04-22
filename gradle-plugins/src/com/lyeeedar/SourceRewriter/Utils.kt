package com.lyeeedar.SourceRewriter

import java.awt.Color
import java.lang.Float.max
import java.lang.Float.min
import java.lang.RuntimeException
import java.security.MessageDigest

fun colourFromStringHash(string: String, targetValue: Float): String
{
	val bytes = MessageDigest.getInstance("MD5").digest(string.toByteArray())
	val color = Color(clamp(bytes[0]), clamp(bytes[1]), clamp(bytes[2]))

	val hsv = RGBToHSV(color.red / 255f, color.green / 255f, color.blue / 255f)

	if (hsv[2] < targetValue)
	{
		hsv[2] = targetValue
	}

	val rgb = HSVToRGB(hsv)

	val r = (rgb[0] * 255).toInt()
	val g = (rgb[1] * 255).toInt()
	val b = (rgb[2] * 255).toInt()

	return "$r,$g,$b"
}

fun clamp(value: Byte): Int
{
	var value = value.toInt() + 126
	if (value < 0) value = 0
	if (value > 255) value = 255
	return value
}

fun HSVToRGB(hsv: FloatArray): FloatArray
{
	val H = hsv[0]
	val S = hsv[1]
	val V = hsv[2]

	val i = H * 6f
	val f = H * 6f - i
	val p = V * (1f - S)
	val q = V * (1f - f * S)
	val t = V * (1f - (1f - f) * S)

	var R: Float
	val G: Float
	val B: Float
	when (i.toInt() % 6)
	{
		0 -> { R = V; G = t; B = p; }
		1 -> { R = q; G = V; B = p; }
		2 -> { R = p; G = V; B = t; }
		3 -> { R = p; G = q; B = V; }
		4 -> { R = t; G = p; B = V; }
		5 -> { R = V; G = p; B = q; }
		else -> throw RuntimeException()
	}

	return floatArrayOf(R, G, B)
}

fun RGBToHSV(R: Float, G: Float, B: Float): FloatArray
{
	val min = min(R, min(G, B))
	val max = max(R, max(G, B))

	var h = max
	var v = max
	val d = max - min
	var s = if (max == 0f) 0f else d / max

	if (max == min)
	{
		h = 0f
	}
	else
	{
		if (max == R)
		{
			h = (G - B) / d + (if (G < B) 6f else 0f)
		}
		else if (max == G)
		{
			h = (B - R) / d + 2f
		}
		else if (max == B)
		{
			h = (R - G) / d + 4f
		}

		h /= 6f
	}

	return floatArrayOf(h, s, v)
}