package com.lyeeedar.headless

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.tools.ktx.KTXProcessor
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.XmlReader
import com.lyeeedar.Util.getChildrenByAttributeRecursively
import ktx.collections.set
import ktx.collections.toGdxArray
import java.io.File
import java.security.MessageDigest


class TextureCompressor
{
	val algorithmVersion = 1

	val doCompression = false

	val processedFiles = ObjectSet<String>()

	val compiledFileHashes = ObjectMap<String, String>()

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Texture Compressor      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		val cacheFilePath = File("../caches/textureCompressionCache")
		if (cacheFilePath.exists())
		{
			val lines = cacheFilePath.readLines()
			for (line in lines)
			{
				if (line.startsWith("algorithm:"))
				{
					val cacheVersion = line.replace("algorithm:", "").toInt()
					if (cacheVersion != algorithmVersion)
					{
						break
					}
				}
				else
				{
					val split = line.split(',')

					val ktxPath = "CompressedData/" + split[0].hashCode().toString() + ".png"
					if (File(ktxPath).exists())
					{
						compiledFileHashes[split[0]] = split[1]
					}
				}
			}
		}

		//findFilesRecursive(File("../assetsraw").absoluteFile)
		//parseCodeFilesRecursive(File("../../core/src").absoluteFile)
		processAtlas()

		var output = "algorithm:$algorithmVersion\n"
		for (cached in compiledFileHashes)
		{
			output += cached.key + "," + cached.value + "\n"
		}
		cacheFilePath.writeText(output)
	}

	private fun processAtlas()
	{
		val atlas = File("../caches/Atlases/SpriteAtlas.atlas")
		val lines = atlas.readLines().toGdxArray()
		for (i in 0 until lines.size-1)
		{
			if (lines[i].endsWith(".png") && lines[i+1].startsWith("size: "))
			{
				val outpath = compressTexture("../caches/Atlases/" + lines[i])
				val relPath = outpath.replace("CompressedData/", "")
				lines[i] = relPath
			}
		}

		val outAtlas = "CompressedData/SpriteAtlas.atlas"
		File(outAtlas).writeText(lines.joinToString("\n"))
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
		val regex = Regex("AssetManager.loadTexture\\(\".*?\"")//(\".*\")")

		val occurances = regex.findAll(contents)

		for (occurance in occurances)
		{
			var path = occurance.value
			path = path.replace("AssetManager.loadTexture(\"", "")
			path = path.replace("\"", "")

			processTexture(path)
		}
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

		spriteElements.addAll(xml.getChildrenByAttributeRecursively("meta:RefKey", "Texture"))
		spriteElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "Texture"))

		for (el in spriteElements)
		{
			val found = processTexture(el)
			if (!found)
			{
				throw RuntimeException("Failed to find texture for file: " + file)
			}
		}
	}

	private fun processTexture(el: XmlReader.Element): Boolean
	{
		return processTexture(el.get("File"))
	}

	private fun processTexture(file: String): Boolean
	{
		var file = file
		if (!file.startsWith("Sprites/")) file = "Sprites/" + file
		if (!file.endsWith(".png")) file += ".png"

		val f = File(file)
		if (!f.exists()) return false

		return compressTexture(file) != null
	}

	private fun compressTexture(file: String): String
	{
		if (processedFiles.contains(file))
		{
			return ""
		}

		System.out.println("Compressing texture '$file'")

		val f = File(file)
		val bytes = f.readBytes()
		val bhash = MessageDigest.getInstance("MD5").digest(bytes)
		val hash = bhash.fold("", { str, it -> str + "%02x".format(it) })

		val outputPath = "CompressedData/" + file.hashCode().toString() + if (doCompression) ".zktx" else ".png"

		if (compiledFileHashes.containsKey(file) && compiledFileHashes[file] == hash)
		{
			System.out.println("Texture '$file' was unchanged. No work required.")
			return outputPath
		}

		compiledFileHashes[file] = hash

		val outFile = File(outputPath)
		if (outFile.exists())
		{
			outFile.delete()
		}

		if (doCompression)
		{
			KTXProcessor.convert(file, outputPath, true, false, false)
		}
		else
		{
			f.copyTo(outFile, true)
		}

		println("Done.")

		processedFiles.add(file)

		return outputPath
	}
}