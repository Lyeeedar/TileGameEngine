package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Pixmap.Format
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.GradientColour
import com.lyeeedar.Renderables.ImageLayer
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.*


object ImageUtils
{
	fun pixmapToImage(pm: Pixmap): BufferedImage
	{
		val image = BufferedImage(pm.width, pm.height, BufferedImage.TYPE_INT_ARGB)
		for (x in 0 until pm.width)
		{
			for (y in 0 until pm.height)
			{
				image.setPixelCol(x, y, pm.getPixelCol(x, y))
			}
		}

		return image
	}

	fun imageToPixmap(image: BufferedImage): Pixmap
	{
		val pixmap = Pixmap(image.width, image.height, Format.RGBA8888)
		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				pixmap.drawPixel(x, y, image.getPixelCol(x, y))
			}
		}

		return pixmap
	}

	fun textureToPixmap(texture: Texture): Pixmap
	{
		if (!texture.textureData.isPrepared)
		{
			texture.textureData.prepare()
		}

		return texture.textureData.consumePixmap()
	}

	fun multiplyPixmap(image: Pixmap, mask: Pixmap): Pixmap
	{
		val pixmap = Pixmap(image.width, image.height, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		val xRatio = mask.width.toFloat() / image.width.toFloat()
		val yRatio = mask.height.toFloat() / image.height.toFloat()

		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				Color.rgba8888ToColor(ca, image.getPixel(x, y))

				val maskX = (x.toFloat() * xRatio).toInt()
				val maskY = (y.toFloat() * yRatio).toInt()

				Color.rgba8888ToColor(cb, mask.getPixel(maskX, maskY))

				ca.mul(cb)

				pixmap.drawPixel(x, y, Color.rgba8888(ca))
			}
		}

		return pixmap
	}

	fun addPixmap(image: Pixmap, mask: Pixmap): Pixmap
	{
		val pixmap = Pixmap(image.width, image.height, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		val xRatio = mask.width.toFloat() / image.width.toFloat()
		val yRatio = mask.height.toFloat() / image.height.toFloat()

		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				Color.rgba8888ToColor(ca, image.getPixel(x, y))

				val maskX = (x.toFloat() * xRatio).toInt()
				val maskY = (y.toFloat() * yRatio).toInt()

				Color.rgba8888ToColor(cb, mask.getPixel(maskX, maskY))

				ca.add(cb)

				pixmap.drawPixel(x, y, Color.rgba8888(ca))
			}
		}

		return pixmap
	}

	fun mergeImages(images: com.badlogic.gdx.utils.Array<ImageLayer>): Pixmap
	{
		var maxWidth = 0
		var maxHeight = 0
		var hasDrawActualSize = false

		for (image in images)
		{
			if (image.modifiers.size > 0)
			{
				val current = image.pixmap

				val originalHeight = current.height

				var temp = pixmapToImage(current)
				current.dispose()

				temp = progressiveScaling(temp, originalHeight * 2)

				for (modifier in image.modifiers)
				{
					modifier.apply(temp)
				}

				temp = progressiveScaling(temp, originalHeight)

				image.pixmap = imageToPixmap(temp)
			}

			var drawWidth = if (image.drawActualSize) image.pixmap.width else 32
			var drawHeight = if (image.drawActualSize) image.pixmap.height else 32

			drawWidth = (drawWidth*image.scale).toInt()
			drawHeight = (drawHeight*image.scale).toInt()

			if (drawWidth > maxWidth)
			{
				maxWidth = drawWidth
			}

			if (drawHeight > maxHeight)
			{
				maxHeight = drawHeight
			}

			hasDrawActualSize = hasDrawActualSize || image.drawActualSize
		}

		val pixmap = Pixmap(maxWidth, maxHeight, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		var first = true
		for (image in images)
		{
			var drawWidth = if (image.drawActualSize) image.pixmap.width else 34
			var drawHeight = if (image.drawActualSize) image.pixmap.height else 34

			drawWidth = (drawWidth*image.scale).toInt()
			drawHeight = (drawHeight*image.scale).toInt()

			val startX = (maxWidth / 2) - (drawWidth / 2)
			val startY = if (hasDrawActualSize) (maxHeight-drawHeight) else (maxHeight / 2) - (drawHeight / 2)

			val xRatio = image.pixmap.width.toFloat() / drawWidth
			val yRatio = image.pixmap.height.toFloat() / drawHeight

			for (x in 0 until drawWidth)
			{
				for (y in 0 until drawHeight)
				{
					val imgX = (x.toFloat() * xRatio).toInt()
					val imgY = (y.toFloat() * yRatio).toInt()

					Color.rgba8888ToColor(ca, pixmap.getPixel(startX+x, startY+y))
					Color.rgba8888ToColor(cb, image.pixmap.getPixel(imgX, imgY))

					val a = if (image.clip && !first) ca.a else ca.a + cb.a
					ca.mul(1f - cb.a)
					cb.mul(cb.a)

					ca.add(cb)
					ca.a = a

					pixmap.drawPixel(startX+x, startY+y, Color.rgba8888(ca))
				}
			}

			first = false
		}

		return pixmap
	}

	fun composeOverhang(base: Pixmap, overhang: Pixmap): Pixmap
	{
		if (base.width != overhang.width || base.height != overhang.height) throw RuntimeException("Incompatible texture sizes for compose overhang!")

		// increase original image by 50% in both axis
		val pixmap = Pixmap((base.width * 1.5f).toInt(), (base.height * 1.5f).toInt(), Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		// draw original to bottom center
		val xOff = base.width / 4
		val yOff = base.height / 2
		for (x in 0 until base.width)
		{
			for (y in 0 until base.height)
			{
				pixmap.drawPixel(xOff + x, yOff + y, base.getPixel(x, y))
			}
		}

		// draw overhang to top center
		for (x in 0 until base.width)
		{
			for (y in 0 until base.height / 2)
			{
				pixmap.drawPixel(xOff + x, y, overhang.getPixel(x, yOff + y))
			}
		}

		return pixmap
	}

	fun resize(input: Pixmap, width: Int, height: Int): Pixmap
	{
		val pixmap = Pixmap(width, height, Format.RGBA8888)

		val xRatio = input.width.toFloat() / width.toFloat()
		val yRatio = input.height.toFloat() / height.toFloat()

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val inputX = (x.toFloat() * xRatio).toInt()
				val inputY = (y.toFloat() * yRatio).toInt()

				pixmap.drawPixel(x, y, input.getPixel(inputX, inputY))
			}
		}

		return pixmap
	}

	fun grayscale(input: BufferedImage)
	{
		// using BT.601
		// Gray = (Red * 0.299 + Green * 0.587 + Blue * 0.114)

		for (x in 0 until input.width)
		{
			for (y in 0 until input.height)
			{
				val col = input.getPixelCol(x, y)

				val gray = col.r * 0.299f + col.g * 0.587f + col.b * 0.114f
				col.set(gray, gray, gray, col.a)

				input.setPixelCol(x, y, col)
			}
		}
	}

	fun floodfill(input: BufferedImage, edgeMap: Array2D<Int>, x: Int, y: Int)
	{
		val stack = Stack<Pair<Point, Int>>()
		stack.push(Point(x, y) to 0)

		while (stack.size > 0)
		{
			val point = stack.pop()
			val x = point.first.x
			val y = point.first.y
			val depth = point.second

			if (x > 0 && y > 0 && x < input.width-1 && y < input.height-1 && edgeMap[x, y] > depth)
			{
				edgeMap[x, y] = depth
				for (dir in Direction.CardinalValues)
				{
					val nx = x + dir.x
					val ny = y + dir.y

					if (edgeMap[nx, ny] > depth+1 && input.getPixelCol(nx, ny).a > 0)
					{
						stack.push(Point(nx, ny) to depth+1)
					}
				}
			}
		}
	}

	fun createEdgeMap(input: BufferedImage): Array2D<Int>
	{
		val edgeMap = Array2D<Int>(input.width, input.height) { _,_ -> Int.MAX_VALUE }

		for (x in 1 until input.width-1)
		{
			for (y in 1 until input.height-1)
			{
				if (input.isEdge(x, y))
				{
					floodfill(input, edgeMap, x, y)
				}
			}
		}

		return edgeMap
	}

	fun tint(image: BufferedImage, tint: Color)
	{
		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				val col = image.getPixelCol(x, y)
				col.mul(tint)

				image.setPixelCol(x, y, col)
			}
		}
	}

	fun stroke(input: BufferedImage, thickness: Int, colour: Color)
	{
		val edgeMap = createEdgeMap(input)

		for (x in 0 until input.width)
		{
			for (y in 0 until input.height)
			{
				var col = input.getPixelCol(x, y)
				val originalA = col.a

				if (col.a > 0)
				{
					if (edgeMap[x, y] < thickness)
					{
						col = colour
					}
					else if (edgeMap[x, y] < thickness * 2)
					{
						col.lerp(colour, 1f - ((edgeMap[x, y]-thickness) / thickness.toFloat()))
					}
				}
				col.a = originalA

				input.setPixelCol(x, y, col)
			}
		}
	}

	fun sample(alpha: Float, colours: Array<GradientColour>): Color
	{
		if (colours.size == 1 || alpha <= colours.first().time)
		{
			return colours.first().colour.color()
		}
		else if (alpha >= colours.last().time)
		{
			return colours.last().colour.color()
		}
		else
		{
			var prev = colours.first()
			var next = colours.first()

			for (col in colours)
			{
				prev = next
				next = col

				if (alpha >= prev.time && alpha <= next.time)
				{
					break
				}
			}

			val localAlpha = (alpha - prev.time) / (next.time - prev.time)
			return prev.colour.color().lerp(next.colour.color(), localAlpha)
		}
	}

	fun internalGradient(input: BufferedImage, colours: Array<GradientColour>)
	{
		val edgeMap = createEdgeMap(input)

		val max = edgeMap.filter { it < Int.MAX_VALUE }.maxOrNull()!!.toFloat()

		for (x in 0 until input.width)
		{
			for (y in 0 until input.height)
			{
				var col = input.getPixelCol(x, y)
				val originalA = col.a

				if (originalA > 0)
				{
					val depth = edgeMap[x, y]
					val alpha = depth.toFloat() / max
					col = sample(alpha, colours)
				}

				col.a = originalA

				input.setPixelCol(x, y, col)
			}
		}
	}

	fun radialGradient(input: BufferedImage, colours: Array<GradientColour>)
	{
		val center = Vector2(input.width / 2f, input.height / 2f)
		val max = center.len()

		for (x in 0 until input.width)
		{
			for (y in 0 until input.height)
			{
				var col = input.getPixelCol(x, y)
				val originalA = col.a

				if (originalA > 0)
				{
					val alpha = center.cpy().sub(x.toFloat(), y.toFloat()).len() / max
					col = sample(alpha, colours)
				}

				col.a = originalA

				input.setPixelCol(x, y, col)
			}
		}
	}

	private fun progressiveScaling(before: BufferedImage, longestSideLength: Int): BufferedImage
	{
		var before: BufferedImage = before
		var w = before.width
		var h = before.height
		var ratio = if (h > w) longestSideLength.toDouble() / h else longestSideLength.toDouble() / w

		//Multi Step Rescale operation
		//This technique is describen in Chris Campbell’s blog The Perils of Image.getScaledInstance(). As Chris mentions, when downscaling to something less than factor 0.5, you get the best result by doing multiple downscaling with a minimum factor of 0.5 (in other words: each scaling operation should scale to maximum half the size).
		while (ratio < 0.5)
		{
			val tmp = scale(before, 0.5)
			before = tmp
			w = before.width
			h = before.height
			ratio = if (h > w) longestSideLength.toDouble() / h else longestSideLength.toDouble() / w
		}
		return scale(before, ratio)
	}

	private fun scale(imageToScale: BufferedImage?, ratio: Double): BufferedImage
	{
		val dWidth = (imageToScale!!.width * ratio).toInt()
		val dHeight = (imageToScale.height * ratio).toInt()
		val scaledImage = BufferedImage(dWidth, dHeight, BufferedImage.TYPE_INT_ARGB)
		val graphics2D = scaledImage.createGraphics()
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
		graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null)
		graphics2D.dispose()
		return scaledImage
	}
}

fun BufferedImage.isEdge(x: Int, y: Int): Boolean
{
	val col = this.getPixelCol(x, y)

	var isEdge = false

	if (col.a > 0f)
	{
		for (dir in Direction.CardinalValues)
		{
			if (this.getPixelCol(x + dir.x, y + dir.y).a == 0f)
			{
				isEdge = true
				break
			}
		}
	}

	return isEdge
}

fun Pixmap.getPixelCol(x: Int, y: Int): Color
{
	val col = Color()
	Color.rgba8888ToColor(col, this.getPixel(x, y))
	return col
}

fun BufferedImage.getPixelCol(x: Int, y: Int): Color
{
	val cc = java.awt.Color(this.getRGB(x, y), true)
	return Color(cc.red / 255f, cc.green / 255f, cc.blue / 255f, cc.alpha / 255f)
}

fun BufferedImage.setPixelCol(x: Int, y: Int, c: Color)
{
	val cc = java.awt.Color(c.r, c.g, c.b, c.a)
	this.setRGB(x, y, cc.rgb)
}

fun Pixmap.drawPixel(x: Int, y: Int, col: Color)
{
	this.drawPixel(x, y, Color.rgba8888(col))
}