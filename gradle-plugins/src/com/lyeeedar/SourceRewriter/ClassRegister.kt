package com.lyeeedar.build.SourceRewriter

import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class ClassRegister(val files: List<File>, val defFolder: File)
{
	val classDefRegex = "(?<IsAbstract>abstract )?class (?<ClassName>[^\\(:<]*)(\\<.*?\\>)?(\\(([^\\(\\)](\\(\\))?)*?\\))?([\\s]*:[\\s]*(?<InheritsFrom>.*))?".toRegex()

	val classDefMap = HashMap<String, ArrayList<ClassDefinition>>()
	val interfaceDefMap = HashMap<String, ArrayList<InterfaceDefinition>>()
	val enumDefMap = HashMap<String, ArrayList<EnumDefinition>>()

	fun <T: BaseTypeDefinition> getDefArray(name: String, sourceType: BaseTypeDefinition, sourceMap: HashMap<String, ArrayList<T>>): ArrayList<T>?
	{
		for (import in sourceType.imports)
		{
			if (import.endsWith(".$name"))
			{
				return sourceMap[import]
			}
		}

		val same = sourceMap["${sourceType.namespace}.$name"]
		if (same != null) { return same }

		for (import in sourceType.imports)
		{
			if (!import.endsWith(".*"))
			{
				val def = sourceMap["$import.$name"]
				if (def != null) { return def }
			}
		}

		for (import in sourceType.imports)
		{
			if (import.endsWith(".*"))
			{
				for (def in sourceMap)
				{
					if (def.key.startsWith(import.substring(0, import.length-1)) && def.key.endsWith(".$name"))
					{
						return def.value
					}
				}
			}
		}

		return null
	}

	fun <T: BaseTypeDefinition> getDef(name: String, sourceType: BaseTypeDefinition, sourceMap: HashMap<String, ArrayList<T>>): T?
	{
		val array = getDefArray(name, sourceType, sourceMap)

		if (array == null || array.size == 0)
		{
			return null
		}
		else if (array.size > 1)
		{
			for (item in array)
			{
				if (item.sourceFile == sourceType.sourceFile)
				{
					return item
				}
			}
		}

		return array[0]
	}

	fun addInterface(item: InterfaceDefinition)
	{
		var array = interfaceDefMap[item.fullName]
		if (array == null)
		{
			array = ArrayList()
			interfaceDefMap[item.fullName] = array
		}

		array.add(item)
	}
	fun addClass(item: ClassDefinition)
	{
		var array = classDefMap[item.fullName]
		if (array == null)
		{
			array = ArrayList()
			classDefMap[item.fullName] = array
		}

		array.add(item)
	}
	fun addEnum(item: EnumDefinition)
	{
		var array = enumDefMap[item.fullName]
		if (array == null)
		{
			array = ArrayList()
			enumDefMap[item.fullName] = array
		}

		array.add(item)
	}

	fun getClass(name: String, sourceType: BaseTypeDefinition): ClassDefinition?
	{
		return getDef(name, sourceType, classDefMap)
	}

	fun getInterface(name: String, sourceType: BaseTypeDefinition): InterfaceDefinition?
	{
		return getDef(name, sourceType, interfaceDefMap)
	}

	fun getEnum(name: String, sourceType: BaseTypeDefinition): EnumDefinition?
	{
		return getDef(name, sourceType, enumDefMap)
	}

	fun registerClasses()
	{
		for (file in files)
		{
			parseFile(file)
		}

		for (classDefArray in classDefMap.values)
		{
			for (classDef in classDefArray)
			{
				var didSuperClass = false
				for (inheritFromRaw in classDef.inheritDeclarations)
				{
					val inheritFrom = inheritFromRaw.split("<")[0]
					if (!didSuperClass)
					{
						didSuperClass = true

						val superClass = getClass(inheritFrom, classDef)
						if (superClass != null)
						{
							classDef.superClass = superClass
						}
						else
						{
							val interfaceDef = getInterface(inheritFrom, classDef)
							if (interfaceDef != null)
							{
								classDef.interfaces.add(interfaceDef)
							}
						}
					}
					else
					{
						val interfaceDef = getInterface(inheritFrom, classDef)
						if (interfaceDef != null)
						{
							classDef.interfaces.add(interfaceDef)
						}
					}
				}
			}
		}

		for (classDefArray in classDefMap.values)
		{
			for (classDef in classDefArray)
			{
				classDef.updateParents()

				classDef.inheritingClasses.sortWith(compareBy { it.name })
				classDef.referencedClasses.sortWith(compareBy { it.name })
			}
		}

		val baseDataClassDef = classDefMap["com.lyeeedar.Util.XmlDataClass"]!!.get(0)
		for (classDef in baseDataClassDef.inheritingClasses)
		{
			classDef.isXmlDataClass = true
		}

		val baseGraphDataClassDef = classDefMap["com.lyeeedar.Util.GraphXmlDataClass"]!!.get(0)
		for (classDef in baseGraphDataClassDef.inheritingClasses)
		{
			classDef.isGraphXmlDataClass = true
		}
	}

	fun parseFile(file: File)
	{
		val lines = file.readLines()

		val imports = ArrayList<String>()
		var packageStr: String = ""
		for (i in 0 until lines.size)
		{
			val line = lines[i]

			val trimmed = line.trim()

			if (trimmed.startsWith("package "))
			{
				packageStr = trimmed.replace("package ", "")
			}
			else if (trimmed.startsWith("import"))
			{
				imports.add(trimmed.replace("import ", "").trim())
			}
			else if (trimmed.startsWith("interface "))
			{
				val name = trimmed.replace("interface ", "").trim()
				val interfaceDef = InterfaceDefinition(name, packageStr, file.canonicalPath)
				interfaceDef.packageStr = packageStr
				interfaceDef.imports.addAll(imports)

				addInterface(interfaceDef)
			}
			else if (trimmed.startsWith("enum class "))
			{
				val name = trimmed.replace("enum class ", "").trim().split(" ")[0].replace("_Mirror", "")

				val enumDef = EnumDefinition(name, packageStr, file.canonicalPath)
				enumDef.imports.addAll(imports)

				var category = ""

				var ii = i+2
				while (true)
				{
					val line = lines[ii]

					val enumValue = line.split(',', '(', ';')[0].trim()

					if (enumValue.startsWith("//"))
					{
						category = enumValue.replace("//", "").trim()
					}
					else if (enumValue.length > 2)
					{
						enumDef.addValue(enumValue, category)
					}

					if (line.contains(';') || line.trim().endsWith('}'))
					{
						break
					}
					ii++
				}

				addEnum(enumDef)
			}
			else if (trimmed.startsWith("data class") || trimmed.startsWith("annotation class") || trimmed.startsWith("open class"))
			{

			}
			else if (trimmed.contains("class "))
			{
				val matches = classDefRegex.matchEntire(trimmed)
				if (matches != null)
				{
					val namedGroups = matches.groups as MatchNamedGroupCollection

					val classDef = ClassDefinition(namedGroups["ClassName"]!!.value.trim(), packageStr, file.canonicalPath)
					classDef.isAbstract = namedGroups["IsAbstract"] != null
					classDef.imports.addAll(imports)

					val inheritsFrom = namedGroups["InheritsFrom"]
					if (inheritsFrom != null)
					{
						val split = inheritsFrom.value.split(",")
						for (other in split)
						{
							classDef.inheritDeclarations.add(other.split("(")[0].trim())
						}
					}

					addClass(classDef)
				}
				else
				{
					System.err.println("Failed to match $trimmed")
				}
			}
		}
	}

	fun writeXmlDefFiles()
	{
		val xmlDataClasses = classDefMap.values.flatten().filter { it.isXmlDataClass }.toList()

		for (dataClass in xmlDataClasses)
		{
			if (dataClass.classDef == null)
			{
				System.err.println("Data class ${dataClass.fullName} failed to match to a definition")
			}
		}

		val rootClasses = xmlDataClasses.filter { it.classDef!!.annotationsRaw.any { it.name == "DataFile" } }.toHashSet()
		rootClasses.addAll(xmlDataClasses.filter { it.classDef!!.forceGlobal })

		val refCountMap = HashMap<ClassDefinition, Int>()
		for (dataClass in rootClasses)
		{
			fun writeRef(classDef: ClassDefinition)
			{
				val referenced = refCountMap.get(classDef)

				if (referenced == null)
				{
					refCountMap.put(classDef, 0)
				}
				else
				{
					refCountMap.put(classDef, referenced+1)
				}
			}

			val startSet = HashSet(rootClasses)
			startSet.remove(dataClass)
			for (referencedClass in dataClass.getAllReferencedClasses(startSet))
			{
				writeRef(referencedClass)
			}
		}

		if (!defFolder.exists()) defFolder.mkdirs()
		val defFolder = defFolder.absolutePath

		// clean folder
		val sharedFolder = File("$defFolder/Shared")
		for (file in sharedFolder.listFiles()?.filterNotNull() ?: ArrayList())
		{
			file.delete()
		}
		for (file in this.defFolder.listFiles()?.filterNotNull() ?: ArrayList())
		{
			file.delete()
		}

		// write root files
		val writtenSpecificFiles = HashSet<ClassDefinition>()
		for (root in rootClasses)
		{
			System.out.println("Writing def file for " + root.fullName)

			val startSet = HashSet(rootClasses)
			startSet.remove(root)

			val otherClasses = HashSet<ClassDefinition>()
			for (referencedClass in root.getAllReferencedClasses(startSet))
			{
				if (rootClasses.contains(referencedClass)) continue

				val refCount = refCountMap[referencedClass] ?: 0
				if (refCount == 0)
				{
					otherClasses.add(referencedClass)
				}
			}

			val dataClassAnnotation = root.classDef!!.annotationsRaw.firstOrNull { it.name == "DataFile" } ?: AnnotationDescription("@DataFile()")
			val name = root.classDef!!.dataClassName
			val colour =  dataClassAnnotation.paramMap["colour"]
			val icon = dataClassAnnotation.paramMap["icon"]

			val builder = IndentedStringBuilder()
			val colourLine = if (colour != null) "Colour=\"$colour\"" else ""
			val iconLine = if (icon != null) "Icon=\"$icon\"" else ""
			builder.appendln(0, "<Definitions $colourLine $iconLine xmlns:meta=\"Editor\">")

			if (writtenSpecificFiles.contains(root)) throw RuntimeException("Class written twice!")
			root.classDef!!.createDefFile(builder, false)
			writtenSpecificFiles.add(root)

			for (classDef in otherClasses.sortedBy { it.name })
			{
				if (classDef.classDef == null)
				{
					throw RuntimeException("Missing class def for " + classDef.fullName)
				}
				if (classDef.classDef!!.forceGlobal) continue

				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
				classDef.classDef!!.createDefFile(builder, false)
				writtenSpecificFiles.add(classDef)

				if (classDef.isAbstract)
				{
					val defNames = HashMap<String, ArrayList<String>>()
					for (childDef in classDef.inheritingClasses)
					{
						if (!childDef.isAbstract)
						{
							var category = defNames[childDef.classDef!!.dataClassCategory]
							if (category == null)
							{
								category = ArrayList()
								defNames[childDef.classDef!!.dataClassCategory] = category
							}
							category.add(childDef.classDef!!.dataClassName)
						}
					}
					val keysStr: String
					if (defNames.size == 1)
					{
						keysStr = defNames.values.first().sorted().joinToString(",")
					}
					else
					{
						keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
					}

					builder.appendln(1, """<Definition Name="${classDef.classDef!!.dataClassName}Defs" Keys="$keysStr" meta:RefKey="ReferenceDef" />""")
				}
			}

			if (root.isAbstract)
			{
				val defNames = HashMap<String, ArrayList<String>>()
				for (childDef in root.inheritingClasses)
				{
					if (!childDef.isAbstract)
					{
						var category = defNames[childDef.classDef!!.dataClassCategory]
						if (category == null)
						{
							category = ArrayList()
							defNames[childDef.classDef!!.dataClassCategory] = category
						}
						category.add(childDef.classDef!!.dataClassName)
					}
				}

				val keysStr: String
				if (defNames.size == 1)
				{
					keysStr = defNames.values.first().sorted().joinToString(",")
				}
				else
				{
					keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
				}

				builder.appendln(1, """<Definition Name="${root.classDef!!.dataClassName}Defs" Keys="$keysStr" IsGlobal="True" meta:RefKey="ReferenceDef" />""")
			}

			builder.appendln(0, "</Definitions>")
			File("$defFolder/$name.xmldef").writeText(builder.toString())

			println("Created def file $name")
		}

		// write shared files
		val sharedClasses = refCountMap.filter { it.value > 0 }.map { it.key }.toHashSet()

		if (sharedClasses.isNotEmpty())
		{
			File("$defFolder/Shared").mkdirs()

			val sharedClassesToWrite = HashSet<ClassDefinition>()

			for (classDef in sharedClasses)
			{
				if (rootClasses.contains(classDef)) continue

				sharedClassesToWrite.add(classDef)
				if (classDef.isAbstract)
				{
					for (childDef in classDef.inheritingClasses)
					{
						if (rootClasses.contains(childDef)) continue
						sharedClassesToWrite.add(childDef)
					}
				}
			}

			for (abstractClass in sharedClassesToWrite.filter { it.isAbstract && it.superClass!!.name.endsWith("XmlDataClass") }.toList())
			{
				val builder = IndentedStringBuilder()
				builder.appendln(0, "<Definitions xmlns:meta=\"Editor\">")

				val defNames = HashMap<String, ArrayList<String>>()

				abstractClass.classDef!!.createDefFile(builder, true)
				sharedClassesToWrite.remove(abstractClass)
				for (classDef in abstractClass.inheritingClasses)
				{
					if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
					classDef.classDef!!.createDefFile(builder, true)
					sharedClassesToWrite.remove(classDef)

					if (!classDef.isAbstract)
					{
						var category = defNames[classDef.classDef!!.dataClassCategory]
						if (category == null)
						{
							category = ArrayList()
							defNames[classDef.classDef!!.dataClassCategory] = category
						}
						category.add(classDef.classDef!!.dataClassName)
					}
				}
				val keysStr: String
				if (defNames.size == 1)
				{
					keysStr = defNames.values.first().sorted().joinToString(",")
				}
				else
				{
					keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
				}

				val abstractClassName = abstractClass.classDef!!.dataClassName
				builder.appendln(1, """<Definition Name="${abstractClassName}Defs" Keys="$keysStr" IsGlobal="True" meta:RefKey="ReferenceDef" />""")

				builder.appendln(0, "</Definitions>")
				File("$defFolder/Shared/${abstractClassName}.xmldef").writeText(builder.toString())
				println("Created def file $abstractClassName")
			}

			for (classDef in sharedClassesToWrite)
			{
				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")

				val builder = IndentedStringBuilder()
				builder.appendln(0, "<Definitions xmlns:meta=\"Editor\">")

				classDef.classDef!!.createDefFile(builder, true)

				builder.appendln(0, "</Definitions>")
				File("$defFolder/Shared/${classDef.classDef!!.dataClassName}.xmldef").writeText(builder.toString())
				println("Created def file ${classDef.classDef!!.dataClassName}")
			}


		}
	}
}

abstract class BaseTypeDefinition(val name: String, val namespace: String, val sourceFile: String)
{
	val imports = ArrayList<String>()

	val fullName: String
		get() = "$namespace.$name"
}

class ClassDefinition(name: String, namespace: String, sourceFile: String): BaseTypeDefinition(name, namespace, sourceFile)
{
	var superClass: ClassDefinition? = null
	val interfaces = ArrayList<InterfaceDefinition>()
	var isAbstract = false

	var inheritDeclarations = ArrayList<String>()

	val inheritingClasses = ArrayList<ClassDefinition>()

	var isXmlDataClass = false
	var isGraphXmlDataClass = false
	var classID: String? = null
	var generatedClassID: String? = null
	var classDef: XmlDataClassDescription? = null
	var referencedClasses = ArrayList<ClassDefinition>()

	fun generateClassID()
	{
		if (generatedClassID != null) return
		if (name.startsWith("Abstract")) return

		// find if any parent is abstract
		var current: ClassDefinition? = superClass
		while (current != null)
		{
			if (current.isAbstract)
			{
				if (!current.name.endsWith("XmlDataClass"))
				{
					val nameBase = current.name.replace("Abstract", "")
					var id = name

					if (id.contains(nameBase))
					{
						id = id.replace(nameBase, "")
					}
					else
					{
						val camelCaseWords = id.split("(?<=.)(?=\\p{Lu})".toRegex())
						id = camelCaseWords.take(camelCaseWords.size-1).joinToString("")
					}

					generatedClassID = "\"$id\""
				}

				break
			}

			current = current.superClass
		}
	}

	fun updateParents(classDef: ClassDefinition? = null)
	{
		if (classDef != null)
		{
			inheritingClasses.add(classDef)
			superClass?.updateParents(classDef)
		}
		else
		{
			superClass?.updateParents(this)
		}
	}

	fun getAllReferencedClasses(processedClasses: HashSet<ClassDefinition>): HashSet<ClassDefinition>
	{
		val output = HashSet<ClassDefinition>()

		output.addAll(referencedClasses)
		output.addAll(inheritingClasses)
		for (classDef in referencedClasses)
		{
			if (!processedClasses.contains(classDef))
			{
				processedClasses.add(classDef)
				output.addAll(classDef.getAllReferencedClasses(processedClasses))
			}
		}
		for (classDef in inheritingClasses)
		{
			output.addAll(classDef.getAllReferencedClasses(processedClasses))
		}

		return output
	}
}

class InterfaceDefinition(name: String, namespace: String, sourceFile: String): BaseTypeDefinition(name, namespace, sourceFile)
{
	var packageStr: String = ""
}

class CategorisedValues(val category: String)
{
	val values = ArrayList<String>()
}
class EnumDefinition(name: String, namespace: String, sourceFile: String): BaseTypeDefinition(name, namespace, sourceFile)
{
	val values = ArrayList<CategorisedValues>()
	private var currentCategory: CategorisedValues? = null

	fun addValue(value: String, category: String)
	{
		if (currentCategory == null || currentCategory!!.category != category)
		{
			currentCategory = CategorisedValues(category)
			values.add(currentCategory!!)
		}

		currentCategory!!.values.add(value)
	}

	fun getAsString(): String
	{
		if (values.size == 1)
		{
			val list = values[0]
			return list.values.joinToString(",") { it.toLowerCase().capitalize() }
		}
		else
		{
			val output = StringBuilder()
			for (category in values)
			{
				if (output.isNotEmpty()) { output.append(",") }
				output.append(category).append("(")
				output.append(category.values.joinToString(",") { it.toLowerCase().capitalize() })
				output.append(")")
			}

			return output.toString()
		}
	}
}