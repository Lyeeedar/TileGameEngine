package com.lyeeedar.headless

import java.io.File
import java.util.zip.ZipFile

class OryxExtractor
{
	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Oryx Extractor      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		val expectedOryxDir = File("../assetsraw/Sprites/Oryx/uf_split")
		if (!expectedOryxDir.exists())
		{
			unzip(File("../../../PrivateStuff/uf_split.zip").absolutePath, File("../assetsraw/Sprites/Oryx").absolutePath)
		}
		else
		{
			println("Oryx already extracted, nothing to do")
		}
	}

	private fun unzip(zipFilePath:String, destDir:String)
	{
		ZipFile(zipFilePath).use { zip ->
			zip.entries().asSequence().forEach { entry ->
				zip.getInputStream(entry).use { input ->
					val outFile = File(destDir + "/" + entry.name)
					if (entry.isDirectory())
					{
						outFile.mkdirs()
					}
					else
					{
						outFile.outputStream().use { output ->
							input.copyTo(output)
						}
					}
				}
			}
		}
	}
}