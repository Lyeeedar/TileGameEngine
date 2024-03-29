package com.lyeeedar.headless

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.XmlReader
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.ImageLayer
import com.lyeeedar.Renderables.RenderedLayeredSprite
import com.lyeeedar.Renderables.Sprite.DirectedSprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.Util.*
import ktx.collections.set
import java.awt.image.BufferedImage
import java.io.File
import java.util.*

/**
 * Created by Philip on 17-Jan-16.
 */
class AtlasCreator
{
	val algorithmVersion = 1

	private val packedPaths = ObjectSet<String>()
	private val localGeneratedImages = ObjectMap<String, BufferedImage>()

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Atlas Creator      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		val atlasFile = File("CompressedData/SpriteAtlas.atlas")
		if (!atlasFile.exists())
		{
			atlasFile.parentFile.mkdirs()
			atlasFile.createNewFile()
		}

		var start = System.currentTimeMillis()

		buildTilingMasksArray()

		println(">>>>>> Parsing resources <<<<<<<<")
		findFilesRecursive(File("../assetsraw").absoluteFile)

		println(">>>>>> Parsing code <<<<<<<<")
		parseCodeFilesRecursive(File("../core/src").absoluteFile)
		parseCodeFilesRecursive(File("../../engine/core/src").absoluteFile)

		println("Parsing completed in ${System.currentTimeMillis() - start}")

		start = System.currentTimeMillis()

		println(">>>>>> Checking Cache <<<<<<<<")
		var doPack = true
		val thisHash = ("algorithm:$algorithmVersion" + packedPaths.sorted().joinToString()).hashCode()

		val cacheFile = File("../caches/atlasPackCache")
		if (cacheFile.exists())
		{
			val cacheHash = cacheFile.readText().toInt()

			if (cacheHash == thisHash)
			{
				System.out.println("Atlas identical, no work to be done.")
				doPack = false
			}
		}

		if (doPack)
		{
			println(">>>>>> Packing <<<<<<<<")

			val outDir = File("../caches/Atlases")
			val contents = outDir.listFiles()
			if (contents != null)
				for (file in contents)
				{
					if (file.path.endsWith(".png"))
					{
						file.delete()
					}
					else if (file.path.endsWith(".atlas"))
					{
						file.delete()
					}
				}

			val settings = TexturePacker.Settings()
			settings.combineSubdirectories = true
			settings.duplicatePadding = true
			settings.maxWidth = 2048
			settings.maxHeight = 2048
			settings.paddingX = 4
			settings.paddingY = 4
			settings.useIndexes = false
			settings.filterMin = Texture.TextureFilter.Nearest
			settings.filterMag = Texture.TextureFilter.Nearest

			val packer = TexturePacker(File("../assetsraw/Sprites"), settings)

			for (path in packedPaths)
			{
				val file = File("../assetsraw/$path")
				if (file.exists())
				{
					packer.addImage(File("../assetsraw/$path"))
				}
				else
				{
					val local = localGeneratedImages[path] ?: error("Failed to pack $path")

					packer.addImage(local, path)
				}
			}

			packer.pack(outDir, "SpriteAtlas")

			cacheFile.writeText(thisHash.toString())
		}

		println("Packing completed in ${System.currentTimeMillis() - start}")
	}

	private fun findFilesRecursive(dir: File)
	{
		val contents = dir.listFiles() ?: return

		for (file in contents)
		{
			if (file.isDirectory)
			{
				findFilesRecursive(file)
			}
			else if (file.path.endsWith(".xml"))
			{
				parseXml(file.path)
			}
		}
	}

	private fun parseCodeFilesRecursive(dir: File)
	{
		val contents = dir.listFiles() ?: return

		for (file in contents)
		{
			if (file.isDirectory)
			{
				parseCodeFilesRecursive(file)
			}
			else
			{
				parseCodeFile(file.path)
			}
		}
	}

	private fun parseCodeFile(file: String)
	{
		val contents = File(file).readText()
		val regex = Regex("AssetManager.loadSprite\\(\".*?\"")//(\".*\")")

		val occurances = regex.findAll(contents)

		for (occurance in occurances)
		{
			var path = occurance.value
			path = path.replace("AssetManager.loadSprite(\"", "")
			path = path.replace("\"", "")

			val found = processSprite(path)
			if (!found && !(path.contains("*") || path.contains("$")))
			{
				throw RuntimeException("Failed to find sprite for file: $path")
			}
		}

		val regex2 = Regex("AssetManager.loadTextureRegion\\(\".*?\"")//(\".*\")")

		val occurances2 = regex2.findAll(contents)

		for (occurance in occurances2)
		{
			var path = occurance.value
			path = path.replace("AssetManager.loadTextureRegion(\"", "")
			path = path.replace("\"", "")

			val found = processSprite(path)
			if (!found && !(path.contains("*") || path.contains("$")))
			{
				throw RuntimeException("Failed to find sprite for file: $path")
			}
		}

		val regex3 = Regex("AssetManager.tryLoadTextureRegion\\(\".*?\"")//(\".*\")")

		val occurances3 = regex3.findAll(contents)

		for (occurance in occurances3)
		{
			var path = occurance.value
			path = path.replace("AssetManager.tryLoadTextureRegion(\"", "")
			path = path.replace("\"", "")

			val found = processSprite(path)
			if (!found && !(path.contains("*") || path.contains("$")))
			{
				throw RuntimeException("Failed to find sprite for file: $path")
			}
		}

		val tilingRegex = Regex("TilingSprite\\(\".*?\", \".*?\", \".*?\"")
		val occurances4 = tilingRegex.findAll(contents)

		for (occurance in occurances4)
		{
			val split = occurance.value.split("\", \"")
			val baseName = split[1]
			val maskName = split[2].subSequence(0, split[2].length-1).toString()

			val succeed = processTilingSprite(baseName, maskName, false)
			if (!succeed)
			{
				throw RuntimeException("Failed to process tilingSprite: base: $baseName mask: $maskName")
			}
		}

		val oryxRegex = Regex("\"Oryx/.*?\"")

		val occurancesOryx = oryxRegex.findAll(contents)

		for (occurance in occurancesOryx)
		{
			var path = occurance.value
			path = path.replace("\"", "")

			val found = processSprite(path)
			if (!found && !(path.contains("*") || path.contains("$")))
			{
				throw RuntimeException("Failed to find sprite for file: $path")
			}
		}

		val regexGray = Regex("AssetManager.loadGrayscaleSprite\\(\".*?\"")//(\".*\")")

		val occurancesGray = regexGray.findAll(contents)

		for (occurance in occurancesGray)
		{
			var path = occurance.value
			path = path.replace("AssetManager.loadGrayscaleSprite(\"", "")
			path = path.replace("\"", "")

			val found = processGrayscaleSprite(path)
			if (!found && !(path.contains("*") || path.contains("$")))
			{
				throw RuntimeException("Failed to find grayscale sprite for file: $path")
			}
		}
	}

	private fun parseXml(file: String)
	{
		val reader = XmlReader()
		var xml: XmlReader.Element? = null

		try
		{
			xml = reader.parse(Gdx.files.internal(file))
		} catch (e: Exception)
		{
			return
		}

		if (xml == null)
		{
			return
		}

		val spriteElements = Array<XmlReader.Element>()

		spriteElements.addAll(xml.getChildrenByAttributeRecursively("meta:RefKey", "Sprite"))
		spriteElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "Sprite"))

		for (el in spriteElements)
		{
			val found = processSprite(el)
			if (!found)
			{
				throw RuntimeException("Failed to find sprite $el for file: $file")
			}
		}

		val tilingSpriteElements = xml.getChildrenByAttributeRecursively("meta:RefKey", "TilingSprite")
		tilingSpriteElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "TilingSprite"))

		for (el in tilingSpriteElements)
		{
			val succeed = processTilingSprite(el)
			if (!succeed)
			{
				throw RuntimeException("Failed to process tiling sprite $el in file: $file")
			}
		}

		val directedSpriteElements = xml.getChildrenByAttributeRecursively("meta:RefKey", "DirectedSprite")
		directedSpriteElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "DirectedSprite"))

		for (el in directedSpriteElements)
		{
			val succeed = processDirectedSprite(el)
			if (!succeed)
			{
				throw RuntimeException("Failed to process directed sprite $el in file: $file")
			}
		}

		val renderedLayeredSpriteElements = xml.getChildrenByAttributeRecursively("meta:RefKey", "RenderedLayeredSprite")
		renderedLayeredSpriteElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "RenderedLayeredSprite"))

		for (el in renderedLayeredSpriteElements)
		{
			val succeed = processLayeredSprite(el)
			if (!succeed)
			{
				throw RuntimeException("Failed to process layered sprite $el in file: $file")
			}
		}

		val maskedTextureElements = xml.getChildrenByAttributeRecursively("meta:RefKey", "MaskedTexture")
		maskedTextureElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "MaskedTexture"))

		for (el in maskedTextureElements)
		{
			val succeed = processMaskedTexture(el)
			if (!succeed)
			{
				throw RuntimeException("Failed to process masked texture $el in file: $file")
			}
		}

		val textureElements = xml.getChildrenByAttributeRecursively("meta:RefKey", "Texture")
		textureElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "Texture"))
		textureElements.addAll(xml.getChildrenByAttributeRecursively("meta:RefKey", "TextureRegion"))
		textureElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "TextureRegion"))

		for (el in textureElements)
		{
			val succeed = tryPackSprite(el.get("File"))
			if (!succeed)
			{
				throw RuntimeException("Failed to process texture $el in file: $file")
			}
		}

		val particleElements = xml.getChildrenByNameRecursively("TextureKeyframes")

		for (el in particleElements)
		{
			val succeed = processParticle(el)
			if (!succeed)
			{
				throw RuntimeException("Failed to process particle $el in file: $file")
			}
		}
	}

	private fun processParticle(xml: XmlReader.Element) : Boolean
	{
		val streamsEl = xml.getChildrenByName("Stream")
		if (streamsEl.size == 0)
		{
			return processParticleStream(xml)
		}
		else
		{
			for (el in streamsEl)
			{
				if (!processParticleStream(el)) return false
			}
		}

		return true
	}

	private fun processParticleStream(xml: XmlReader.Element) : Boolean
	{
		for (i in 0..xml.childCount-1)
		{
			val el = xml.getChild(i)
			var path: String

			if (el.text != null)
			{
				val split = el.text.split("|")
				path = split[1]
			}
			else
			{
				path = el.get("Value")
			}

			val found = processSprite(path)
			if (!found) return false
		}

		return true
	}

	private fun processDirectedSprite(spriteElement: XmlReader.Element): Boolean
	{
		var found = false

		val name = spriteElement.text

		for (dir in DirectedSprite.allDirs)
		{
			val path = name + "_" + dir
			val done = processSprite(path)

			if (done) found = true
		}

		return found
	}

	private fun processTilingSprite(spriteElement: XmlReader.Element): Boolean
	{
		val topElement = spriteElement.getChildByName("Top")
		val directions = spriteElement.getChildByName("Directions")
		if (topElement != null)
		{
			// Predefined sprite

			val overhangElement = spriteElement.getChildByName("Overhang")
			val frontElement = spriteElement.getChildByName("Front")

			var exists = tryPackSprite(topElement)
			if (!exists)
			{
				return false
			}

			exists = tryPackSprite(frontElement)
			if (!exists)
			{
				return false
			}

			if (overhangElement != null)
			{

				// pack top overhang
				exists = packOverhang(topElement.get("Name"), overhangElement.get("Name"))
				if (!exists)
				{
					return false
				}

				// pack front overhang
				exists = packOverhang(frontElement.get("Name"), overhangElement.get("Name"))
				if (!exists)
				{
					return false
				}
			}
		}
		else if (directions != null)
		{
			// Predefined sprite

			for (el in directions.children())
			{
				val exists = tryPackSprite(el)
				if (!exists)
				{
					return false
				}
			}
		}
		else
		{
			// Auto masking sprites
			val spriteDataElement = spriteElement.getChildByName("Sprite")

			val texName = spriteDataElement.get("Name")
			val maskName = spriteElement.get("Mask")
			val additive = spriteElement.getBoolean("Additive", false)

			val succeed = processTilingSprite(texName, maskName, additive)
			if (!succeed)
			{
				return false
			}
		}

		return true
	}

	private fun processTilingSprite(baseName: String, maskBaseName: String, additive: Boolean): Boolean
	{
		for (mask in tilingMasks)
		{
			val succeed = maskSprite(baseName, maskBaseName, mask, additive)

			if (!succeed)
			{
				return false
			}
		}

		return true
	}

	private fun packOverhang(topName: String, overhangName: String) : Boolean
	{
		val composedName = "${topName}_Overhang_$overhangName"

		// File exists on disk, no need to compose
		if (tryPackSprite(composedName))
		{
			println("Added Overhang sprite: " + composedName)
			return true
		}

		val topHandle = Gdx.files.internal("../assetsraw/Sprites/$topName.png")
		if (!topHandle.exists())
		{
			System.err.println("Failed to find sprite for: " + topName)
			return false
		}

		val overhangHandle = Gdx.files.internal("../assetsraw/Sprites/$overhangName.png")
		if (!overhangHandle.exists())
		{
			System.err.println("Failed to find sprite for: " + overhangName)
			return false
		}

		val top = Pixmap(topHandle)
		val overhang = Pixmap(overhangHandle)
		val composed = ImageUtils.composeOverhang(top, overhang)
		top.dispose()
		overhang.dispose()

		val image = ImageUtils.pixmapToImage(composed)
		composed.dispose()

		localGeneratedImages[composedName] = image
		packedPaths.add(composedName)
		return true
	}

	private fun maskSprite(baseName: String, maskBaseName: String, masks: Array<String>, additive: Boolean): Boolean
	{
		// Build the mask suffix
		var mask = ""
		for (m in masks)
		{
			mask += "_" + m
		}

		val maskedName = baseName + "_" + maskBaseName + mask + "_" + additive

		// File exists on disk, no need to mask
		if (tryPackSprite(maskedName))
		{
			println("Added Tiling sprite: " + maskedName)
			return true
		}

		val baseHandle = Gdx.files.internal("../assetsraw/Sprites/$baseName.png")
		if (!baseHandle.exists())
		{
			System.err.println("Failed to find sprite for: " + baseName)
			return false
		}

		val base = Pixmap(baseHandle)

		var merged = if (additive) Pixmap(base.width, base.height, Pixmap.Format.RGBA8888) else base
		for (maskSuffix in masks)
		{
			var maskHandle = Gdx.files.internal("../assetsraw/Sprites/" + maskBaseName + "_" + maskSuffix + ".png")
			if (!maskHandle.exists())
			{
				maskHandle = Gdx.files.internal("../assetsraw/Sprites/" + maskBaseName + "_C.png")
			}

			if (!maskHandle.exists())
			{
				maskHandle = Gdx.files.internal("../assetsraw/Sprites/$maskBaseName.png")
			}

			if (!maskHandle.exists())
			{
				System.err.println("Failed to find mask for: " + maskBaseName + "_" + maskSuffix)
				return false
			}

			val maskPixmap = Pixmap(maskHandle)
			val currentPixmap = if (additive) base else merged

			val maskedTex = ImageUtils.multiplyPixmap(currentPixmap, maskPixmap)

			if (additive)
			{
				val addedText = ImageUtils.addPixmap(merged, maskedTex)
				merged.dispose()
				maskedTex.dispose()

				merged = addedText
			} else
			{
				if (merged !== base)
				{
					merged.dispose()
				}
				merged = maskedTex
			}
		}

		val image = ImageUtils.pixmapToImage(merged)
		merged.dispose()

		localGeneratedImages[maskedName] = image
		packedPaths.add(maskedName)

		println("Added Tiling sprite: " + maskedName)

		return true
	}

	private fun tryPackSprite(element: XmlReader.Element): Boolean
	{
		val name = element.get("Name")
		val exists = tryPackSprite(name)
		if (!exists)
		{
			System.err.println("Could not find sprites with name: " + name)
			return false
		} else
		{
			println("Added sprites for name: " + name)
			return true
		}
	}

	private fun tryPackSprite(name: String): Boolean
	{
		var path = name
		if (!path.startsWith("Sprites/")) path = "Sprites/$path"
		if (!path.endsWith(".png")) path += ".png"

		if (packedPaths.contains(path))
		{
			return true
		}

		val handle = Gdx.files.internal("../assetsraw/$path")

		if (handle.exists())
		{
			packedPaths.add(path)
			return true
		}
		else
		{
			return false
		}
	}

	private fun processSprite(spriteElement: XmlReader.Element): Boolean
	{
		val name = spriteElement.get("Name", null) ?: return true

		return processSprite(name)
	}

	private fun processSprite(name: String): Boolean
	{
		var foundCount = 0

		// Try sprite without indexes
		val exists = tryPackSprite(name)
		if (exists)
		{
			foundCount++
		}

		// Try 0 indexed sprite
		if (foundCount == 0)
		{
			var i = 0
			while (true)
			{
				val exists = tryPackSprite(name + "_" + i)
				if (!exists)
				{
					break
				}
				else
				{
					foundCount++
				}

				i++
			}
		}

		// Try 1 indexed sprite
		if (foundCount == 0)
		{
			var i = 1
			while (true)
			{
				val exists = tryPackSprite(name + "_" + i)
				if (!exists)
				{
					break
				} else
				{
					foundCount++
				}

				i++
			}
		}

		if (foundCount == 0)
		{
			System.err.println("Could not find sprites with name: " + name)
		}
		else
		{
			println("Added sprites for name: " + name)
		}

		return foundCount > 0
	}

	private fun processGrayscaleSprite(basename: String): Boolean
	{
		val name = basename + "_grayscale"
		if (tryPackSprite(name))
		{
			println("Grayscale sprite already exists: $name")
			return true
		}

		val fileHandle = Gdx.files.internal("../assetsraw/Sprites/$basename.png")
		if (!fileHandle.exists())
		{
			System.err.println("Failed to find sprite layer: $basename")
			return false
		}

		val pixmap = Pixmap(fileHandle)
		val image = ImageUtils.pixmapToImage(pixmap)
		ImageUtils.grayscale(image)

		pixmap.dispose()

		localGeneratedImages[name] = image
		packedPaths.add(name)

		println("Added grayscale sprite: $name")

		return true
	}

	private fun processLayeredSprite(spriteElement: XmlReader.Element): Boolean
	{
		val renderedLayeredSprite = RenderedLayeredSprite()
		renderedLayeredSprite.load(XmlData.loadFromElement(spriteElement))

		val mergedName = renderedLayeredSprite.toString()

		if (tryPackSprite(mergedName))
		{
			println("Layered sprite already exists: $mergedName")
			return true
		}

		for (layer in renderedLayeredSprite.layers)
		{
			val fileHandle = Gdx.files.internal("../assetsraw/Sprites/${layer.path}.png")
			if (!fileHandle.exists())
			{
				System.err.println("Failed to find sprite layer: ${layer.path}")
				return false
			}

			layer.pixmap = Pixmap(fileHandle)
		}

		val merged = ImageUtils.mergeImages(renderedLayeredSprite.layers)

		val image = ImageUtils.pixmapToImage(merged)
		merged.dispose()
		for (layer in renderedLayeredSprite.layers)
		{
			layer.pixmap.dispose()
		}

		localGeneratedImages[mergedName] = image
		packedPaths.add(mergedName)

		println("Added layered sprite: $mergedName")

		return true
	}

	private fun processMaskedTexture(element: XmlReader.Element): Boolean
	{
		for (el in element.children())
		{
			if (!tryPackSprite(el.text))
			{
				return false
			}
		}

		return true
	}

	companion object
	{

		var tilingMasks = Array<Array<String>>()
		fun buildTilingMasksArray()
		{
			val directions = HashSet<Direction>()
			for (dir in Direction.Values)
			{
				directions.add(dir)
			}

			val powerSet = powerSet(directions)

			val alreadyAdded = HashSet<String>()

			for (set in powerSet)
			{
				val bitflag = EnumBitflag<Direction>()
				for (dir in set)
				{
					bitflag.setBit(dir)
				}

				val masks = TilingSprite.getMasks(bitflag)
				var mask = ""
				for (m in masks)
				{
					mask += "_" + m
				}

				if (!alreadyAdded.contains(mask))
				{
					tilingMasks.add(masks)
					alreadyAdded.add(mask)
				}
			}
		}

		fun <T> powerSet(originalSet: Set<T>): Set<Set<T>>
		{
			val sets = HashSet<Set<T>>()
			if (originalSet.isEmpty())
			{
				sets.add(HashSet<T>())
				return sets
			}
			val list = ArrayList(originalSet)
			val head = list[0]
			val rest = HashSet(list.subList(1, list.size))
			for (set in powerSet(rest))
			{
				val newSet = HashSet<T>()
				newSet.add(head)
				newSet.addAll(set)
				sets.add(newSet)
				sets.add(set)
			}
			return sets
		}
	}
}
