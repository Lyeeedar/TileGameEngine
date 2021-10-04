package com.lyeeedar.SourceRewriter

import com.lyeeedar.build.SourceRewriter.ClassRegister
import com.lyeeedar.build.SourceRewriter.IndentedStringBuilder
import java.io.File


class ComponentType
{
	companion object
	{
		fun write(classRegister: ClassRegister, srcDir: File)
		{
			println("Write component types")
			val destPath = File(srcDir.absolutePath + "/../../../../../game/core/src/com/lyeeedar/Components/ComponentType.kt").canonicalFile

			val enumBuilder = IndentedStringBuilder()
			val extensionsBuilder = IndentedStringBuilder()

			val rootDef = classRegister.classDefMap["com.lyeeedar.Components.AbstractComponent"]!![0]
			for (def in rootDef.inheritingClasses.sortedBy { it.name })
			{
				if (def.isAbstract) continue

				val className = def.name
				val enumName = className.replace("Component", "")

				enumBuilder.appendln(1, "$enumName({ $className() }),")

				val extensionName = if (enumName.length < 4) enumName.toLowerCase() else enumName.substring(0, 1).toLowerCase() + enumName.substring(1)

				extensionsBuilder.appendln("inline fun Entity.$extensionName(): $className? = this.components[ComponentType.$enumName.ordinal] as $className?")
			}

			val output = IndentedStringBuilder()
			output.appendln("package com.lyeeedar.Components")
			output.appendln("")
			output.appendln("actual enum class ComponentType private constructor(val constructor: ()->AbstractComponent)")
			output.appendln("{")
			output.appendln(enumBuilder.toString().trimEnd().dropLast(1) + ";")
			output.appendln("")
			output.appendln(1, "companion object")
			output.appendln(1, "{")
			output.appendln(2, "val Values = ComponentType.values()")
			output.appendln(2, "val Temporary = arrayOf( MarkedForDeletion, Transient )")
			output.appendln(1, "}")
			output.appendln("}")
			output.appendln("")
			output.appendln(extensionsBuilder.toString())

			destPath.writeText(output.toString())

			println("Wrote component types to " + destPath.canonicalPath)
		}
	}
}