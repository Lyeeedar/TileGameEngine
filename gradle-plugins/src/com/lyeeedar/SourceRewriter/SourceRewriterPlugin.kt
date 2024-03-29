package com.lyeeedar.build

import com.lyeeedar.SourceRewriter.ComponentType
import com.lyeeedar.build.SourceRewriter.ClassRegister
import com.lyeeedar.build.SourceRewriter.IndentedStringBuilder
import com.lyeeedar.build.SourceRewriter.SourceRewriter
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.RuntimeException

//gradlew :core:build  --stacktrace -Dorg.gradle.daemon=false   -Dorg.gradle.debug=true
class SourceRewriterPlugin : Plugin<Project>
{
	override fun apply(target: Project)
	{
		target.tasks.create("rewriteSources", SourceRewriterTask::class.java)
	}
}

open class SourceRewriterTask : DefaultTask()
{
	@InputFiles
	var inputDirs: Set<File>? = null

	@OutputDirectories
	var srcDirs: Set<File>? = null

	@TaskAction
	fun rewriteSources()
	{
		try
		{
			println("################################################")
			println("Source Rewrite starting")
			println("")

			println("Parsing source files")
			val srcFiles = find(srcDirs!!)

			val defsDir = File(srcDirs!!.first().absolutePath + "/../../../../../game/assetsraw/Definitions/Generated").canonicalFile

			val classRegister = ClassRegister(srcFiles, defsDir)
			classRegister.registerClasses()

			val dataClassFiles = ArrayList<SourceRewriter>()
			for (file in srcFiles)
			{
				val rewriter = SourceRewriter(file, classRegister)
				val hasDataClass = rewriter.parse()
				if (hasDataClass)
				{
					dataClassFiles.add(rewriter)
				}
			}

			val loaderBuilder = IndentedStringBuilder()

			println("Writing changes")
			for (rewriter in dataClassFiles)
			{
				try
				{
					rewriter.write(loaderBuilder)
				}
				catch (ex: Exception)
				{
					throw RuntimeException("Failed to write " + rewriter.file.canonicalPath, ex)
				}
			}

			writeXmlLoader(loaderBuilder)

			ComponentType.write(classRegister, srcDirs!!.first())

			println("Writing def files")
			classRegister.writeXmlDefFiles()

			println("")
			println("Source Rewrite completed")
			println("################################################")
		}
		catch (ex: Exception)
		{
			ex.printStackTrace()
			throw ex
		}
	}

	private fun writeXmlLoader(builder: IndentedStringBuilder)
	{
		println("Writing xml loader")
		val destPath = File(srcDirs!!.first().absolutePath + "/../../../../../game/core/src/com/lyeeedar/Util/XmlDataClassLoader.kt").canonicalFile
		val output = IndentedStringBuilder()
		output.appendln("package com.lyeeedar.Util")
		output.appendln("")
		output.appendln("")
		output.appendln("actual class XmlDataClassLoader")
		output.appendln("{")
		output.appendln(1, "companion object")
		output.appendln(1, "{")

		output.appendln(builder.toString())

		output.appendln(1, "}")
		output.appendln("}")

		destPath.writeText(output.toString())
		println("Wrote loader to " + destPath.canonicalPath)
	}

	private fun find(roots: Set<File>): List<File>
	{
		val files: MutableList<File> = ArrayList()
		for (root in roots)
		{
			if (!root.isDirectory) throw IllegalAccessError("$root must be a folder.")
			addFiles(files, root)
		}
		return files
	}

	private fun addFiles(files: MutableList<File>, folder: File)
	{
		for (f in folder.listFiles())
		{
			if (f.isFile && f.name.endsWith(".kt"))
			{
				files.add(f)
			}
			else if (f.isDirectory)
			{
				addFiles(files, f)
			}
		}
	}
}