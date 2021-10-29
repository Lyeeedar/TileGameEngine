package com.lyeeedar.headless

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.ObjectMap
import ktx.collections.set
import java.io.File

class GlslCompiler
{
	val vertexHeader = """
		
	""".trimIndent()

	val fragmentHeader = """
		#ifdef GL_ES
		#define LOWP lowp
		precision mediump float;
		#else
		#define LOWP
		#endif
		
	""".trimIndent()

	fun expandConstants(shader: String): String
	{
		var shader = shader
		shader = shader.replace("\${ShaderProgram.POSITION_ATTRIBUTE}", ShaderProgram.POSITION_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.COLOR_ATTRIBUTE}", ShaderProgram.COLOR_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.TEXCOORD_ATTRIBUTE}", ShaderProgram.TEXCOORD_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.NORMAL_ATTRIBUTE}", ShaderProgram.NORMAL_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.BINORMAL_ATTRIBUTE}", ShaderProgram.BINORMAL_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.BONEWEIGHT_ATTRIBUTE}", ShaderProgram.BONEWEIGHT_ATTRIBUTE)
		shader = shader.replace("\${ShaderProgram.TANGENT_ATTRIBUTE}", ShaderProgram.TANGENT_ATTRIBUTE)

		return shader
	}

	fun restoreOldPrecision(shader: String, original: String): String
	{
		var shader = shader
		shader = shader.replace("precision mediump float;", "")

		val replacementMap = ObjectMap<String, String>()
		for (line in original.lines())
		{
			val line = line.trim()

			if (line.contains("LOWP"))
			{
				replacementMap[line.replace("LOWP ", "")] = line
			}
			else if (line.contains("lowp"))
			{
				replacementMap[line.replace("lowp ", "")] = line
			}
			else if (line.contains("mediump"))
			{
				replacementMap[line.replace("mediump ", "")] = line
			}
		}

		for (pair in replacementMap.entries())
		{
			shader = shader.replace(pair.key, pair.value)
		}

		return shader
	}

	fun optimiseShader(context: GlslOptimiser.glslopt_ctx, type: Int, input: String): String
	{
		val input = expandConstants(input)

		val compiled = GlslOptimiser.INSTANCE.glslopt_optimize(context, type, input, 0)
		val output = GlslOptimiser.INSTANCE.glslopt_get_output(compiled)

		if (output == null)
		{
			val log = GlslOptimiser.INSTANCE.glslopt_get_log(compiled)
			System.err.println("Failed to compile shader")
			System.err.println("\n--------------------------------------------------------------------\n")
			System.err.println(input)
			System.err.println("\n--------------------------------------------------------------------\n")
			System.err.println(log)
		}

		GlslOptimiser.INSTANCE.glslopt_shader_delete(compiled)

		return restoreOldPrecision(output, input)
	}

	fun addHeader(shader: String, header: String): String
	{
		var shader = shader
		if (shader.contains("#version 300 es"))
		{
			shader = shader.replace("#version 300 es", "#version 300 es\n$header\n")
		}
		else
		{
			shader = header + shader
		}

		return shader
	}

	fun optimiseFragment(context: GlslOptimiser.glslopt_ctx, input: String): String
	{
		val optimised = optimiseShader(context, GlslOptimiser.glslopt_shader_type.kGlslOptShaderFragment, input)
		return addHeader(optimised, fragmentHeader)
	}

	fun optimiseVertex(context: GlslOptimiser.glslopt_ctx, input: String): String
	{
		val optimised = optimiseShader(context, GlslOptimiser.glslopt_shader_type.kGlslOptShaderVertex, input)
		return addHeader(optimised, vertexHeader)
	}

	fun writeToOutput(shader: String, file: File)
	{
		var dst = file.relativeTo(File("../assetsraw").absoluteFile).path.replace("\\", "/")
		dst = "CompressedData/${dst.hashCode()}." + file.extension

		File(dst).writeText(shader)
	}

	private fun findFilesRecursive(context: GlslOptimiser.glslopt_ctx, dir: File)
	{
		val contents = dir.listFiles() ?: return

		for (file in contents)
		{
			if (file.isDirectory)
			{
				findFilesRecursive(context, file)
			}
			else if (file.path.endsWith(".vert"))
			{
				System.out.println("Processing ${file.path}")

				val optimised = optimiseVertex(context, file.readText())
				writeToOutput(optimised, file)
			}
			else if (file.path.endsWith(".frag"))
			{
				System.out.println("Processing ${file.path}")

				val optimised = optimiseFragment(context, file.readText())
				writeToOutput(optimised, file)
			}
		}
	}

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Shader Compiler      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("Running in directory: " + File("").absolutePath)
		println("")
		println("")

		var start = System.currentTimeMillis()

		val context = GlslOptimiser.INSTANCE.glslopt_initialize(
			GlslOptimiser.glslopt_target.kGlslTargetOpenGLES30)

		try
		{
			println(">>>>>> Parsing resources <<<<<<<<")
			findFilesRecursive(context, File("../assetsraw").absoluteFile)
		}
		finally
		{
			GlslOptimiser.INSTANCE.glslopt_cleanup(context)
		}

		println("Optimising completed in ${System.currentTimeMillis() - start}")
	}
}