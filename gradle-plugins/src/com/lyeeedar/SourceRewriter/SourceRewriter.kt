package com.lyeeedar.build.SourceRewriter

import java.io.File

class SourceRewriter(val file: File, val classRegister: ClassRegister)
{
	val variableRegex = "(override )?(?<VariableType>var|val|lateinit var) (?<Name>[a-zA-Z0-9]*)(: (?<Type>[a-zA-Z0-9<>, ?*]*))?(= (?<DefaultValue>.*))?".toRegex()

	lateinit var originalContents: String
	val dataClasses = ArrayList<XmlDataClassDescription>()

	val fileContents = ArrayList<IFilePart>()

	fun parse(): Boolean
	{
		originalContents = file.readText()

		val lines = originalContents.split('\n')

		val packagePart = PackageFilePart()
		val importsPart = ImportsFilePart()

		fileContents.add(packagePart)
		fileContents.add(importsPart)

		var doneImports = false
		var currentMiscPart: MiscFilePart? = null
		var currentClassPart: DataClassFilePart? = null
		var currentFuncDepth: Int = 0
		var inGeneratedPart = false
		var inNonDataPart = false
		var annotations: ArrayList<AnnotationDescription>? = null
		for (line in lines)
		{
			val trimmed = line.trim()
			if (currentClassPart == null)
			{
				if (line.startsWith("package "))
				{
					packagePart.packageStr = line.trim()
					continue
				}
				else if (line.startsWith("import "))
				{
					importsPart.imports.add(line.trim())
					doneImports = true
					continue
				}
				else if (trimmed.startsWith("class ") || trimmed.startsWith("abstract class "))
				{
					val name = trimmed.split(':', '<', '(')[0].split("class ")[1].trim()
					val namespace = packagePart.packageStr.replace("package ", "")
					val classDefinition = classRegister.classDefMap["$namespace.$name"]?.get(0)
					if (classDefinition == null)
					{
						System.err.println("Failed to find class def for $namespace.$name")
					}

					if (classDefinition?.isXmlDataClass == true)
					{
						currentMiscPart = null

						currentClassPart = DataClassFilePart(
							name,
							trimmed,
							line.length - line.trimStart().length,
							classDefinition,
							classRegister,
							annotations ?: ArrayList())
						annotations = null

						dataClasses.add(currentClassPart.desc)

						fileContents.add(currentClassPart)

						currentFuncDepth = if (trimmed.endsWith("{")) 1 else 0
						inGeneratedPart = false
						inNonDataPart = false
						doneImports = true

						continue
					}
				}
				else if (trimmed.startsWith("@") && !trimmed.startsWith("@JvmField"))
				{
					if (annotations == null)
					{
						annotations = ArrayList()
					}
					annotations.add(AnnotationDescription(trimmed))

					doneImports = true
					continue
				}

				if (trimmed.isNotEmpty())
				{
					doneImports = true
				}

				if (currentMiscPart == null)
				{
					currentMiscPart = MiscFilePart()
					fileContents.add(currentMiscPart)
				}

				if (doneImports)
				{
					currentMiscPart.code.add(line.trimEnd())
				}

				annotations = null
			}
			else
			{
				if (trimmed.contains("{"))
				{
					currentFuncDepth++

					if (currentFuncDepth == 1)
					{
						continue
					}
				}
				if (trimmed.contains("}"))
				{
					currentFuncDepth--

					if (currentFuncDepth == 0)
					{
						println("Found data class ${currentClassPart.desc.name}")

						currentClassPart = null
						annotations = null
						continue
					}
				}

				if (currentFuncDepth == 1)
				{
					if (trimmed.startsWith("@"))
					{
						if (annotations == null)
						{
							annotations = ArrayList()
						}

						annotations.add(AnnotationDescription(trimmed))
					}
					else if (!inNonDataPart && !inGeneratedPart && annotations?.any { it.name == "Transient" } != true)
					{
						val matches = variableRegex.matchEntire(trimmed)
						if (matches != null)
						{
							val namedGroups = matches.groups as MatchNamedGroupCollection
							val variableType = when (namedGroups["VariableType"]!!.value)
							{
								"val" -> VariableType.VAL
								"var" -> VariableType.VAR
								"lateinit var" -> VariableType.LATEINIT
								else -> throw RuntimeException("Unknown variable type ${namedGroups["VariableType"]!!.value}")
							}
							val name = namedGroups["Name"]!!.value
							val type = namedGroups["Type"]?.value ?: "String"
							val default = namedGroups["DefaultValue"]?.value ?: ""

							val variableDesc = VariableDescription(variableType, name, type.trim(), default, trimmed, annotations ?: ArrayList())
							currentClassPart.desc.variables.add(variableDesc)

							if (variableDesc.variableType == VariableType.VAL && variableDesc.name == "classID")
							{
								currentClassPart.desc.classDefinition.classID = variableDesc.defaultValue
							}

							annotations = null
						}
						else
						{
							annotations = null
						}
					}
				}
				else
				{
					annotations = null
				}

				if (trimmed == "//region generated")
				{
					inGeneratedPart = true
					continue
				}
				else if (inGeneratedPart && trimmed == "//endregion")
				{
					inGeneratedPart = false
					continue
				}
				else if (trimmed == "//region non-data")
				{
					inNonDataPart = true
				}
				else if (inNonDataPart && trimmed == "//endregion")
				{
					inNonDataPart = false
				}

				if (!inGeneratedPart)
				{
					currentClassPart.desc.classContents.add(line.trimEnd())
				}
			}
		}

		return true
	}

	fun write(loaderBuilder: IndentedStringBuilder)
	{
		if (!fileContents.any { it is DataClassFilePart } || file.nameWithoutExtension == "XmlData") return

		val imports = fileContents[1] as ImportsFilePart
		for (part in fileContents)
		{
			if (part is DataClassFilePart)
			{
				part.desc.resolveImports(imports.imports)
			}
		}

		val output = IndentedStringBuilder()
		for (part in fileContents)
		{
			part.write(output, loaderBuilder)
		}

		val newContents = output.toString().trim()
		if (newContents != originalContents)
		{
			file.writeText(newContents)

			println("Writing ${file.absolutePath} complete")
		}
		else
		{
			println("Skipping writing ${file.absolutePath}. Identical")
		}
	}
}