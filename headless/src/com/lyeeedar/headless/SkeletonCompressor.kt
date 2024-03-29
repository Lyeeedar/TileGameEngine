package com.lyeeedar.headless

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.XmlReader
import com.lyeeedar.Renderables.SkeletonData
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getChildrenByAttributeRecursively
import java.io.File

class SkeletonCompressor
{
	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Skeleton Compressor      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		var start = System.currentTimeMillis()

		println(">>>>>> Parsing resources <<<<<<<<")
		findFilesRecursive(File("../assetsraw").absoluteFile)

		println("Compressing completed in ${System.currentTimeMillis() - start}")
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

		val skeletonElements = Array<XmlReader.Element>()

		skeletonElements.addAll(xml.getChildrenByAttributeRecursively("meta:RefKey", "Skeleton"))
		skeletonElements.addAll(xml.getChildrenByAttributeRecursively("RefKey", "Skeleton"))

		if (xml.name == "Skeleton" && xml.hasAttribute("xmlns:meta"))
		{
			skeletonElements.add(xml)
		}

		try
		{
			for (el in skeletonElements)
			{
				processSkeleton(el)
			}
		}
		catch (ex: Exception)
		{
			System.err.println("Failed to compress skeletons from $file")
			throw ex
		}
	}

	private fun processSkeleton(el: XmlReader.Element)
	{
		val data = SkeletonData()
		data.load(XmlData.loadFromElement(el))

		var dst = data.path.replace("\\", "/")
		dst = "CompressedData/${dst.hashCode()}"

		val src = "../assetsraw/" + data.path
		val srcFolder = src.removeSuffix("/" + src.split("/").last())

		val atlasSrc = File("$src.atlas")
		val atlasDst = File("$dst.atlas")
		val jsonSrc = File("$src.skel")
		val jsonDst = File("$dst.skel")

		// copy
		atlasSrc.copyTo(atlasDst, true)
		jsonSrc.copyTo(jsonDst, true)

		// copy images and fix atlas
		var atlasContents = atlasDst.readText()
		atlasDst.forEachLine {
			if (it.endsWith(".png"))
			{
				val imgPath = "$srcFolder/$it"
				val imgDstName = "${imgPath.hashCode()}.png"

				val imgSrc = File(imgPath)
				val imgDst = File("CompressedData/$imgDstName")

				imgSrc.copyTo(imgDst, true)
				atlasContents = atlasContents.replace(it, imgDstName)
			}
		}
		atlasDst.writeText(atlasContents)

		System.out.println("Found skeleton ${data.path}")
	}
}