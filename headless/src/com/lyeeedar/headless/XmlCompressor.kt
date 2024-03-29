package com.lyeeedar.headless

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getRawXml
import java.io.File

class XmlCompressor
{
	val rootPath: String

	val processedPaths = Array<String>()

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Xml Compressor      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		rootPath = File("../assetsraw").absolutePath

		// clear out existing data
		val outputFolder = File("CompressedData")
		if (!outputFolder.exists())
		{
			outputFolder.mkdirs()
		}

		for (file in outputFolder.list())
		{
			if (file.endsWith(".xmldata"))
			{
				val deleted = File("CompressedData/" + file).delete()
				if (!deleted)
				{
					error("Failed to delete file $file!")
				}
			}
		}

		findFilesRecursive(File("../assetsraw").absoluteFile, true)

		var processedPathsFile = "<Paths>\n"

		for (path in processedPaths)
		{
			processedPathsFile += "\t<Path>$path</Path>\n"
		}

		processedPathsFile += "</Paths>"

		val tempFile = File("../assetsraw/ProcessedPaths.xml")
		tempFile.writeText(processedPathsFile)
		processXml(tempFile.absolutePath)
	}

	private fun findFilesRecursive(dir: File, isRoot: Boolean)
	{
		val contents = dir.listFiles() ?: return

		for (file in contents)
		{
			if (file.isDirectory)
			{
				findFilesRecursive(file, false)
			}
			else if (!isRoot && file.path.endsWith(".xml"))
			{
				processXml(file.path)
			}
		}
	}

	private fun processXml(path: String)
	{
		val root = rootPath.replace("\\", "/")
		val relativePath = path.replace("\\", "/").replace("$root/", "")

		val rawxml = getRawXml("../assetsraw/$relativePath")
		val data = XmlData.loadFromElement(rawxml)

		val outputPath = "CompressedData/" + relativePath.hashCode() + ".xmldata"

		data.save(outputPath)

		// try to load it
		XmlData().load(outputPath)

		processedPaths.add(relativePath)
		System.out.println("Compressed $relativePath")
	}
}