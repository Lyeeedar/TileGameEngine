package com.lyeeedar.build.SourceRewriter

import com.lyeeedar.SourceRewriter.colourFromStringHash
import javax.swing.text.html.StyleSheet


enum class VariableType
{
    VAR,
    VAL,
    LATEINIT
}

val assetManagerLoadedTypes = setOf("ParticleEffect", "ParticleEffectDescription",
                                    "Sprite", "SpriteWrapper", "DirectionalSprite",
                                    "Sound",
                                    "Colour",
                                    "Light",
                                    "Texture", "TextureRegion",
                                    "Renderable")
class VariableDescription(val variableType: VariableType, val name: String, val type: String, val defaultValue: String, val raw: String, val annotations: ArrayList<AnnotationDescription>)
{
	val dataName: String
	var visibleIfStr: String = ""

	init
	{
		val dataValueAnnotation = annotations.firstOrNull { it.name == "DataValue" }
		if (dataValueAnnotation != null)
		{
			dataName = dataValueAnnotation.paramMap["dataName"]?.replace("\"", "") ?: name.capitalize()

			var visibleIfRaw = dataValueAnnotation.paramMap["visibleIf"]
			if (visibleIfRaw != null)
			{
				visibleIfRaw = visibleIfRaw.replace("&", "&amp;")
				visibleIfRaw = visibleIfRaw.replace("<", "&lt;")
				visibleIfRaw = visibleIfRaw.replace(">", "&gt;")
				visibleIfStr = """VisibleIf="$visibleIfRaw""""
			}
		}
		else
		{
			dataName = name.capitalize()
		}
	}

    fun resolveImports(imports: HashSet<String>, classDefinition: ClassDefinition, classRegister: ClassRegister)
    {
		var type = type
		var nullable = false
		if (type.endsWith('?'))
		{
			type = type.substring(0, type.length-1)
			nullable = true
		}

        if (assetManagerLoadedTypes.contains(type))
        {
            imports.add("import com.lyeeedar.Util.AssetManager")
        }
        else if (type == "String" || type == "Int" || type == "Float" || type == "Boolean")
        {
            // primitives dont need imports
        }
		else if (type == "Point" || type == "Vector2" || type == "Vector3"|| type == "Vector4")
		{

		}
		else if (classRegister.getEnum(type, classDefinition) != null)
		{
			imports.add("import java.util.*")
		}
		else if (type.startsWith("FastEnumMap<"))
        {
	        imports.add("import java.util.*")
        }
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)
			if (assetManagerLoadedTypes.contains(arrayType))
			{
				imports.add("import com.lyeeedar.Util.AssetManager")
			}
			else if (classRegister.getEnum(type, classDefinition) != null)
			{
				imports.add("import java.util.*")
			}
			else if (arrayType == "Point" && annotations.any { it.name == "DataAsciiGrid" })
			{
				imports.add("import com.lyeeedar.Util.toHitPointArray")
			}
			else
			{
				val classDef = classRegister.getClass(arrayType, classDefinition)
				if (classDef?.classDef != null && classDef.isAbstract)
				{
					imports.add("import com.lyeeedar.Util.XmlDataClassLoader")
				}
			}
		}
	    else
        {
	        val classDef = classRegister.getClass(type, classDefinition)
	        if (classDef?.classDef != null && classDef.isAbstract)
	        {
		        imports.add("import com.lyeeedar.Util.XmlDataClassLoader")
	        }
        }
    }

    fun writeLoad(builder: IndentedStringBuilder, indentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister, extraVariables: ArrayList<String>)
    {
	    writeLoad(builder, indentation, classDefinition, classRegister, extraVariables, type, variableType, name)
    }

	fun writeLoad(builder: IndentedStringBuilder, indentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister, extraVariables: ArrayList<String>, type: String, variableType: VariableType, targetName: String, sourceElName: String? = null)
	{
		val name = targetName

        var type = type
        var nullable = false
        if (type.endsWith('?'))
        {
            type = type.substring(0, type.length-1)
            nullable = true
        }

		val get = if (classDefinition.classDef!!.getAnnotations().any { it.name == "DataClassCollection" }) "getAttribute" else "get"

        if (type == "String")
        {
            if (variableType == VariableType.LATEINIT)
            {
                builder.appendln(indentation, "$name = xmlData.$get(\"$dataName\")")
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = xmlData.$get(\"$dataName\", $defaultValue)"
                if (!nullable)
                {
                    loadLine += "!!"
                }
                builder.appendln(indentation, loadLine)
            }
        }
        else if (type == "Char")
        {
	        if (variableType == VariableType.LATEINIT)
	        {
		        builder.appendln(indentation, "$name = xmlData.$get(\"$dataName\")[0]")
	        }
	        else if (variableType == VariableType.VAR)
	        {
		        if (nullable)
		        {
			        builder.appendln(indentation, "${name}Raw = xmlData.$get(\"$dataName\", ${defaultValue.replace("'", "\"")})")
			        builder.appendln(indentation, "if (${name}Raw != null) $name = ${name}Raw[0]")
		        }
		        else
		        {
			        builder.appendln(indentation, "$name = xmlData.$get(\"$dataName\", ${defaultValue.replace("'", "\"")})!![0]")
		        }
	        }
        }
        else if (type == "Int")
        {
            if (variableType == VariableType.VAR)
            {
                builder.appendln(indentation, "$name = xmlData.${get}Int(\"$dataName\", $defaultValue)")
            }
        }
        else if (type == "Float")
        {
            if (variableType == VariableType.VAR)
            {
                builder.appendln(indentation, "$name = xmlData.${get}Float(\"$dataName\", $defaultValue)")
            }
        }
		else if (type == "Boolean")
		{
			if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = xmlData.${get}Boolean(\"$dataName\", $defaultValue)")
			}
		}
		else if (type == "Point" || type == "Vector2" || type == "Vector3" || type == "Vector4")
		{
			if (variableType == VariableType.VAR || variableType == VariableType.LATEINIT)
			{
				if (variableType == VariableType.LATEINIT)
				{
					builder.appendln(indentation, """val ${name}Raw = xmlData.${get}("$dataName").split(',')""")
				}
				else
				{
					var defaultValue = defaultValue.split('(')[1].dropLast(1)
					if (defaultValue.isBlank())
					{
						if (type == "Vector4")
						{
							defaultValue = "0,0,0,0"
						}
						else if (type == "Vector3")
						{
							defaultValue = "0,0,0"
						}
						else
						{
							defaultValue = "0,0"
						}
					}
					builder.appendln(indentation, """val ${name}Raw = xmlData.${get}("$dataName", "$defaultValue")!!.split(',')""")
				}

				if (type == "Point")
				{
					builder.appendln(indentation, "$name = Point(${name}Raw[0].trim().toInt(), ${name}Raw[1].trim().toInt())")
				}
				else if (type == "Vector2")
				{
					builder.appendln(indentation, "$name = Vector2(${name}Raw[0].trim().toFloat(), ${name}Raw[1].trim().toFloat())")
				}
				else if (type == "Vector3")
				{
					builder.appendln(indentation, "$name = Vector3(${name}Raw[0].trim().toFloat(), ${name}Raw[1].trim().toFloat(), ${name}Raw[2].trim().toFloat())")
				}
				else
				{
					builder.appendln(indentation, "$name = Vector3(${name}Raw[0].trim().toFloat(), ${name}Raw[1].trim().toFloat(), ${name}Raw[2].trim().toFloat(), ${name}Raw[3].trim().toFloat())")
				}
			}
		}
        else if (assetManagerLoadedTypes.contains(type))
        {
			val loadName: String
			if (type == "ParticleEffectDescription" || type == "ParticleEffect")
			{
				loadName = "ParticleEffect"
			}
			else
			{
				loadName = type
			}

			val loadExtension: String
			if (type == "ParticleEffect")
			{
				if (variableType == VariableType.LATEINIT)
				{
					loadExtension = ".getParticleEffect()"
				}
				else if (nullable)
				{
					loadExtension = "?.getParticleEffect()"
				}
				else
				{
					loadExtension = ""
				}
			}
			else
			{
				loadExtension = ""
			}

            if (variableType == VariableType.LATEINIT)
            {
                val loadLine = "$name = AssetManager.load$loadName(xmlData.getChildByName(\"$dataName\")!!)"
                builder.appendln(indentation, "$loadLine$loadExtension")
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = AssetManager.tryLoad$loadName(xmlData.getChildByName(\"$dataName\"))"
                if (!nullable)
                {
                    loadLine += "!!"
                }
                builder.appendln(indentation, "$loadLine$loadExtension")
            }
        }
		else if (type == "CompiledExpression")
        {
	        val annotation = annotations.firstOrNull { it.name == "DataCompiledExpression" }
	        if (annotation != null)
	        {
		        val createMethod = annotation.paramMap["createExpressionMethod"]
		        if (createMethod != null)
		        {
			        if (variableType == VariableType.LATEINIT || !nullable)
			        {
				        builder.appendln(indentation, "$name = $createMethod(xmlData.${get}(\"$dataName\"))")
			        }
			        else
			        {
				        builder.appendln(indentation, "$name = $createMethod(xmlData.${get}(\"$dataName\", null))")
			        }
		        }
		        else
		        {
			        if (variableType == VariableType.LATEINIT || !nullable)
			        {
				        builder.appendln(indentation, "$name = CompiledExpression(xmlData.${get}(\"$dataName\"))")
			        }
			        else
			        {
				        builder.appendln(indentation, "val ${name}String = xmlData.${get}(\"$dataName\", null)")
				        builder.appendln(indentation, "$name = if (${name}String != null) CompiledExpression(${name}String) else null")
			        }
		        }
	        }
	        else
	        {
		        if (variableType == VariableType.LATEINIT || !nullable)
		        {
			        builder.appendln(indentation, "$name = CompiledExpression(xmlData.${get}(\"$dataName\", \"1\")!!)")
		        }
		        else
		        {
			        builder.appendln(indentation, "val ${name}String = xmlData.${get}(\"$dataName\", null)")
			        builder.appendln(indentation, "$name = if (${name}String != null) CompiledExpression(${name}String) else null")
		        }
	        }
        }
		else if (type == "XmlData")
        {
	        val annotation = annotations.first { it.name == "DataXml" }
	        val actualClass = annotation.paramMap["actualClass"]!!

	        val classDef = classRegister.getClass(actualClass, classDefinition) ?: throw RuntimeException("createDefEntry: Unknown type '$actualClass'!")
	        classDefinition.referencedClasses.add(classDef)

	        var line = "$name = xmlData.getChildByName(\"$dataName\")"
	        if (variableType == VariableType.LATEINIT || !nullable)
	        {
		        line += "!!"
	        }

	        builder.appendln(indentation, line)
        }
		else if (classRegister.getEnum(type, classDefinition) != null)
		{
			val enumDef = classRegister.getEnum(type, classDefinition)!!

			if (variableType == VariableType.LATEINIT)
			{
				builder.appendln(indentation, "$name = ${enumDef.name}.valueOf(xmlData.${get}(\"$dataName\").toUpperCase(Locale.ENGLISH))")
			}
			else if (sourceElName != null)
			{
				builder.appendln(indentation, "$name = ${enumDef.name}.valueOf($sourceElName.text.toUpperCase(Locale.ENGLISH))")
			}
			else if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = ${enumDef.name}.valueOf(xmlData.${get}(\"$dataName\", ${defaultValue}.toString())!!.toUpperCase(Locale.ENGLISH))")
			}
		}
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)

			val elName = name+"El"

			if (classDefinition.classDef!!.getAnnotations().any { it.name == "DataClassCollection" })
			{
				builder.appendln(indentation, "val $elName = xmlData")
			}
			else if (sourceElName != null)
			{
				builder.appendln(indentation, "val $elName = $sourceElName")
			}
			else
			{
				builder.appendln(indentation, "val $elName = xmlData.getChildByName(\"$dataName\")")
			}

			if (arrayType == "Point")
			{
				if (annotations.any { it.name == "DataAsciiGrid" })
				{
					builder.appendln(indentation, "if ($elName != null) $name.addAll($elName.toHitPointArray())")
					builder.appendln(indentation, "else $name.add(Point(0, 0))")
				}
				else
				{
					throw RuntimeException("Non-ascii grid arrays of points not supported")
				}
			}
			else
			{
				val timelineAnnotation = annotations.firstOrNull { it.name == "DataTimeline" }
				val isTimelineGroup = timelineAnnotation != null && timelineAnnotation.paramMap["timelineGroup"] == "true"

				builder.appendln(indentation, "if ($elName != null)")
				builder.appendln(indentation, "{")
				builder.appendln(indentation + 1, "for (el in ${elName}.children)")
				builder.appendln(indentation + 1, "{")

				if (arrayType == "String")
				{
					builder.appendln(indentation + 2, "$name.add(el.text)")
				}
				else if (arrayType == "Int")
				{
					builder.appendln(indentation + 2, "$name.add(el.int())")
				}
				else if (arrayType == "Float")
				{
					builder.appendln(indentation + 2, "$name.add(el.float())")
				}
				else if (arrayType == "Boolean")
				{
					builder.appendln(indentation + 2, "$name.add(el.boolean())")
				}
				else if (isTimelineGroup)
				{
					val classDef = classRegister.getClass(arrayType, classDefinition)
					               ?: throw RuntimeException("writeLoad: Unknown type '$arrayType' in '$type'!")
					classDefinition.referencedClasses.add(classDef)

					builder.appendln(indentation + 2, "for (keyframeEl in el.children)")
					builder.appendln(indentation + 2, "{")
					if (classDef.isAbstract)
					{
						builder.appendln(indentation + 3, "val obj$name = XmlDataClassLoader.load$arrayType(keyframeEl.get(\"classID\", keyframeEl.name)!!)")
					}
					else
					{
						builder.appendln(indentation + 3, "val obj$name = $arrayType()")
					}

					builder.appendln(indentation + 3, "obj$name.load(keyframeEl)")
					builder.appendln(indentation + 3, "$name.add(obj$name)")
					builder.appendln(indentation + 2, "}")
				}
				else
				{
					builder.appendln(indentation+2, "val obj$name: $arrayType")
					writeLoad(builder, indentation+2, classDefinition, classRegister, extraVariables, arrayType, VariableType.VAR, "obj$name", "el")
					builder.appendln(indentation+2, "$name.add(obj$name)")
				}

				builder.appendln(indentation + 1, "}")
				builder.appendln(indentation, "}")

				if (isTimelineGroup)
				{
					builder.appendln(indentation, "$name.sort(compareBy{ it.time })")
				}
			}
		}
		else if (type.startsWith("ObjectMap<"))
        {
	        val dataNodesAnnotation = annotations.firstOrNull { it.name == "DataGraphNodes" }
	        if (dataNodesAnnotation != null)
	        {
		        val elName = name+"El"

		        val nodeType = type.split(",")[1].split(">")[0].trim()

		        builder.appendln(indentation, "val $elName = xmlData.getChildByName(\"$dataName\")")
		        builder.appendln(indentation, "if ($elName != null)")
		        builder.appendln(indentation, "{")
		        builder.appendln(indentation+1, "for (el in ${elName}.children)")
		        builder.appendln(indentation+1, "{")

		        val classDef = classRegister.getClass(nodeType, classDefinition) ?: throw RuntimeException("writeLoad: Unknown type '$nodeType' in '$type'!")
		        classDefinition.referencedClasses.add(classDef)

		        if (classDef.isAbstract)
		        {
			        builder.appendln(indentation+2, "val obj = XmlDataClassLoader.load$nodeType(el.get(\"classID\", el.name)!!)")
		        }
		        else
		        {
			        builder.appendln(indentation+2, "val obj = $nodeType()")
		        }

		        builder.appendln(indentation+2, "obj.load(el)")
		        builder.appendln(indentation+2, "val guid = el.getAttribute(\"GUID\")")
		        builder.appendln(indentation+2, "$name[guid] = obj")

		        builder.appendln(indentation+1, "}")
		        builder.appendln(indentation, "}")
	        }
	        else
	        {
		        throw RuntimeException("ObjectMap that isnt a @DataGraphNodes not currently supported")
	        }
        }
		else if (type.startsWith("FastEnumMap<"))
		{
			val rawTypeParams = type.replace("FastEnumMap<", "").dropLast(1).split(",")
			val enumType = rawTypeParams[0].trim()
			val valueType = rawTypeParams[1].trim()

			val enumDef = classRegister.getEnum(enumType, classDefinition) ?: throw RuntimeException("Unable to find definition for enum for $type")

			val elName = name+"El"
			builder.appendln(indentation, "val $elName = xmlData.getChildByName(\"$dataName\")")

			builder.appendln(indentation, "if ($elName != null)")
			builder.appendln(indentation, "{")
			builder.appendln(indentation + 1, "for (el in ${elName}.children)")
			builder.appendln(indentation + 1, "{")

			builder.appendln(indentation + 2, "val enumVal = $enumType.valueOf(el.name.toUpperCase(Locale.ENGLISH))")

			if (valueType == "String")
			{
				builder.appendln(indentation + 2, "$name[enumVal] = el.text")
			}
			else if (valueType == "Int")
			{
				builder.appendln(indentation + 2, "$name[enumVal] = el.int()")
			}
			else if (valueType == "Float")
			{
				builder.appendln(indentation + 2, "$name[enumVal] = el.float()")
			}
			else if (valueType == "Boolean")
			{
				builder.appendln(indentation + 2, "$name[enumVal] = el.boolean()")
			}
			else
			{
				var creation = ""
				if (valueType.startsWith("Array<"))
				{
					creation = " = Array()"
				}

				builder.appendln(indentation+2, "val obj$name: $valueType$creation")
				writeLoad(builder, indentation+2, classDefinition, classRegister, extraVariables, valueType, VariableType.VAR, "obj$name", "el")
				builder.appendln(indentation+2, "$name[enumVal] = obj$name")
			}

			builder.appendln(indentation + 1, "}")
			builder.appendln(indentation, "}")
		}
        else
        {
            val classDef = classRegister.getClass(type, classDefinition) ?: throw RuntimeException("writeLoad: Unknown type '$type'!")

            classDefinition.referencedClasses.add(classDef)

	        val graphAnnotation = annotations.firstOrNull { it.name == "DataGraphReference" }
	        if (graphAnnotation != null)
	        {
				if (variableType == VariableType.LATEINIT)
				{
					extraVariables.add("private lateinit var ${name}GUID: String")
					builder.appendln(indentation, "${name}GUID = xmlData.get(\"$dataName\")")
				}
		        else
				{
					extraVariables.add("private var ${name}GUID: String? = null")
					builder.appendln(indentation, "${name}GUID = xmlData.get(\"$dataName\", null)")
				}
	        }
	        else
	        {
		        val el = name + "El"
		        if (variableType == VariableType.LATEINIT || (variableType == VariableType.VAR && !nullable))
		        {
			        if (sourceElName != null)
			        {
				        builder.appendln(indentation, "val $el = $sourceElName")
			        }
			        else
			        {
				        builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")!!")
			        }

			        if (classDef.isAbstract)
			        {
				        builder.appendln(indentation, "$name = XmlDataClassLoader.load$type(${el}.get(\"classID\", ${el}.name)!!)")
			        }
			        else
			        {
				        builder.appendln(indentation, "$name = $type()")
			        }

			        builder.appendln(indentation, "$name.load($el)")
		        }
		        else if (variableType == VariableType.VAR)
		        {
			        if (sourceElName != null)
			        {
				        builder.appendln(indentation, "val $el = $sourceElName")
			        }
			        else
			        {
				        builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")")
			        }
			        builder.appendln(indentation, "if ($el != null)")
			        builder.appendln(indentation, "{")

			        if (classDef.isAbstract)
			        {
				        builder.appendln(indentation + 1, "$name = XmlDataClassLoader.load$type(${el}.get(\"classID\", ${el}.name)!!)")
			        }
			        else
			        {
				        builder.appendln(indentation + 1, "$name = $type()")
			        }

			        builder.appendln(indentation + 1, "$name!!.load($el)")
			        builder.appendln(indentation, "}")
		        }
		        else
		        {
			        if (sourceElName != null)
			        {
				        builder.appendln(indentation, "val $el = $sourceElName")
			        }
			        else
			        {
				        builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")!!")
			        }
			        builder.appendln(indentation, "$name.load($el)")
		        }
	        }
        }
    }

	fun writeResolve(builder: IndentedStringBuilder, indentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister)
	{
		var type = type
		var nullable = false
		if (type.endsWith('?'))
		{
			type = type.substring(0, type.length-1)
			nullable = true
		}

		val annotation = annotations.firstOrNull { it.name == "DataGraphReference" }
		if (annotation != null)
		{
			if (variableType == VariableType.LATEINIT || !nullable)
			{
				builder.appendln(indentation, "$name = nodes[${name}GUID]!!")
			}
			else
			{
				builder.appendln(indentation, "if (!${name}GUID.isNullOrBlank()) $name = nodes[${name}GUID]!!")
			}
		}
		else if (type.startsWith("ObjectMap<"))
		{
			val dataNodesAnnotation = annotations.firstOrNull { it.name == "DataGraphNodes" }
			if (dataNodesAnnotation != null)
			{
				if (nullable)
				{
					builder.appendln(indentation, "if ($name != null)")
					builder.appendln(indentation, "{")
					builder.appendln(indentation+1, "for (item in $name.values())")
					builder.appendln(indentation+1, "{")
					builder.appendln(indentation+2, "item.resolve(nodes)")
					builder.appendln(indentation+1, "}")
					builder.appendln(indentation, "}")
				}
				else
				{
					builder.appendln(indentation, "for (item in $name.values())")
					builder.appendln(indentation, "{")
					builder.appendln(indentation+1, "item.resolve(nodes)")
					builder.appendln(indentation, "}")
				}
			}
		}
		else
		{
			// recurse into children
			if (type.startsWith("Array<"))
			{
				val arrayType = type.replace("Array<", "").dropLast(1)
				val classDef = classRegister.getClass(arrayType, classDefinition)
				if (classDef != null && classDef.isGraphXmlDataClass)
				{
					if (nullable)
					{
						builder.appendln(indentation, "if ($name != null)")
						builder.appendln(indentation, "{")
						builder.appendln(indentation+1, "for (item in $name)")
						builder.appendln(indentation+1, "{")
						builder.appendln(indentation+2, "item.resolve(nodes)")
						builder.appendln(indentation+1, "}")
						builder.appendln(indentation, "}")
					}
					else
					{
						builder.appendln(indentation, "for (item in $name)")
						builder.appendln(indentation, "{")
						builder.appendln(indentation+1, "item.resolve(nodes)")
						builder.appendln(indentation, "}")
					}
				}
			}
			else
			{
				val classDef = classRegister.getClass(type, classDefinition)
				if (classDef != null && classDef.isGraphXmlDataClass)
				{
					if (nullable)
					{
						builder.appendln(indentation, "$name?.resolve(nodes)")
					}
					else
					{
						builder.appendln(indentation, "$name.resolve(nodes)")
					}
				}
			}
		}
	}

    fun createDefEntry(indentation: Int, builder: IndentedStringBuilder, classDefinition: ClassDefinition, classRegister: ClassRegister)
    {
	    if (variableType == VariableType.VAL && name == "classID")
	    {
		    val defaultValue = defaultValue.replace("\"", "")
		    builder.appendln(indentation, """<Const Name="classID">$defaultValue</Const>""")
		    return
	    }

	    createDefEntry(builder, indentation, classDefinition, classRegister, type, variableType, dataName)
    }

	fun createDefEntry(builder: IndentedStringBuilder, indentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister, type: String, variableType: VariableType, dataName: String)
	{
        var type = type
        var isNullable = false
        if (type.endsWith('?'))
        {
            type = type.substring(0, type.length-1)
            isNullable = true
        }

        var nullable = ""
        var skipIfDefault = ""

		if (type == this.type)
		{
			if (variableType == VariableType.LATEINIT)
			{
				nullable = """Nullable="False""""
				skipIfDefault = """SkipIfDefault="False""""
			}
			else if (variableType == VariableType.VAL)
			{
				nullable = """Nullable="False""""
				skipIfDefault = """SkipIfDefault="False""""
			}
			else
			{
				if (isNullable)
				{
					nullable = """Nullable="True""""
					skipIfDefault = """SkipIfDefault="True""""
				}
				else
				{
					nullable = """Nullable="False""""
					skipIfDefault = """SkipIfDefault="False""""
				}
			}
		}

        if (type == "String")
        {
			val canSkip = if (variableType != VariableType.LATEINIT) "True" else "False"
			val defaultValue = if (type != this.type || this.defaultValue.isBlank() || this.defaultValue == "null") "\"\"" else this.defaultValue

			val fileReferenceAnnotation = annotations.firstOrNull { it.name == "DataFileReference" }
			if (fileReferenceAnnotation != null)
			{
				val basePath = fileReferenceAnnotation.paramMap["basePath"]
				val stripExtension = fileReferenceAnnotation.paramMap["stripExtension"]
				val resourceType = fileReferenceAnnotation.paramMap["resourceType"]
				val allowedFileTypes = fileReferenceAnnotation.paramMap["allowedFileTypes"]

				val basePathStr = if (basePath != null) "BasePath=\"$basePath\"" else ""
				val stripExtensionStr = if (stripExtension != null) "StripExtension=\"$stripExtension\"" else "StripExtension=\"True\""
				val resourceTypeStr = if (resourceType != null) "ResourceType=\"$resourceType\"" else ""
				val allowedFileTypesStr = if (allowedFileTypes != null) "AllowedFileTypes=\"$allowedFileTypes\"" else ""

				builder.appendlnFix(indentation, """<Data Name="$dataName" $basePathStr $stripExtensionStr $resourceTypeStr $allowedFileTypesStr SkipIfDefault="$canSkip" Default=$defaultValue $visibleIfStr meta:RefKey="File" />""")
			}
			else
			{
				val localisationAnnotation = annotations.firstOrNull { it.name == "DataNeedsLocalisation" }
				val needsLocalisation = if (localisationAnnotation != null) "NeedsLocalisation=\"True\"" else ""
				val localisationFile = if (localisationAnnotation != null) "LocalisationFile=\"${localisationAnnotation.paramMap["file"]!!}\"" else ""

				builder.appendlnFix(indentation, """<Data Name="$dataName" $needsLocalisation $localisationFile SkipIfDefault="$canSkip" Default=$defaultValue $visibleIfStr meta:RefKey="String" />""")
			}
        }
	    else if (type == "Char")
	    {
		    val canSkip = if (variableType != VariableType.LATEINIT) "True" else "False"
		    val defaultValue = if (type != this.type || this.defaultValue.isBlank() || this.defaultValue == "null") "\"\"" else this.defaultValue

		    builder.appendlnFix(indentation, """<Data Name="$dataName" SkipIfDefault="$canSkip" MaxLength="1" Default=$defaultValue $visibleIfStr  meta:RefKey="String" />""")
	    }
        else if (type == "Int" || type == "Float")
        {
            val numericAnnotation = annotations.firstOrNull { it.name == "DataNumericRange" }
            val min = numericAnnotation?.paramMap?.get("min")?.replace("f", "")
            val max = numericAnnotation?.paramMap?.get("max")?.replace("f", "")
            val minStr = if (min != null) """Min="$min"""" else ""
            val maxStr = if (max != null) """Max="$max"""" else ""
			val defaultValue = if (type != this.type) "0" else this.defaultValue.replace("f", "")

            builder.appendlnFix(indentation, """<Data Name="$dataName" $minStr $maxStr Type="$type" Default="$defaultValue" SkipIfDefault="True" $visibleIfStr meta:RefKey="Number" />""")
        }
		else if (type == "Point" || type == "Vector2" || type == "Vector3" || type == "Vector4")
		{
			val numericAnnotation = annotations.firstOrNull { it.name == "DataNumericRange" }
			val min = numericAnnotation?.paramMap?.get("min")?.replace("f", "")
			val max = numericAnnotation?.paramMap?.get("max")?.replace("f", "")
			val minStr = if (min != null) """Min="$min"""" else ""
			val maxStr = if (max != null) """Max="$max"""" else ""
			val defaultValue = if (type != this.type) "0" else this.defaultValue.split('(')[1].dropLast(1).replace("f", "")
			val numericType = if (type == "Point") "Type=\"Int\"" else ""

			val vectorAnnotation = annotations.firstOrNull { it.name == "DataVector" }
			val name1 = vectorAnnotation?.paramMap?.get("name1")
			val name2 = vectorAnnotation?.paramMap?.get("name2")
			val name3 = vectorAnnotation?.paramMap?.get("name3")
			val name4 = vectorAnnotation?.paramMap?.get("name4")
			val name1Str = if (name1 != null) "Name1=\"$name1\"" else ""
			val name2Str = if (name2 != null) "Name2=\"$name2\"" else ""
			val name3Str = if (name3 != null) "Name3=\"$name3\"" else ""
			val name4Str = if (name4 != null) "Name4=\"$name4\"" else ""
			val numComponents = if (type == "Point") "NumComponents=\"2\"" else "NumComponents=\"${type.last()}\""

			builder.appendlnFix(indentation, """<Data Name="$dataName" $minStr $maxStr $numericType $name1Str $name2Str $name3Str $name4Str $numComponents SkipIfDefault="True" Default="$defaultValue" $visibleIfStr meta:RefKey="Vector" />""")
		}
		else if (type == "Boolean")
		{
			val defaultValue = if (type != this.type) "False" else this.defaultValue

			builder.appendlnFix(indentation, """<Data Name="$dataName" SkipIfDefault="True" Default="$defaultValue" $visibleIfStr meta:RefKey="Boolean" />""")
		}
        else if (type == "Colour")
        {
	        val defaultValue = if (type != this.type) "" else this.defaultValue
	        var defaultColour = ""
	        if (defaultValue.contains("Colour."))
	        {
		        val colName = defaultValue.split(".")[1]

		        val c = StyleSheet().stringToColor(colName)
		        defaultColour = "Default=\"${c.red},${c.green},${c.blue}\""
	        }

	        builder.appendlnFix(indentation, """<Data Name="$dataName" SkipIfDefault="false" $defaultColour $visibleIfStr meta:RefKey="Colour" />""")
        }
        else if (assetManagerLoadedTypes.contains(type))
        {
			val dataType = when (type)
			{
				"ParticleEffectDescription" -> "ParticleEffect,ParticleEffectTemplate"
				"Renderable" -> "Sprite,TilingSprite,ParticleEffect"
				else -> type
			}
            builder.appendlnFix(indentation, """<Data Name="$dataName" Keys="$dataType" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
        }
		else if (type == "CompiledExpression")
        {
	        var default = "1"
	        var tooltip = ""

	        val annotation = annotations.firstOrNull { it.name == "DataCompiledExpression" }
	        if (annotation != null)
	        {
		        if (annotation.paramMap["default"] != null)
		        {
			        default = annotation.paramMap["default"]!!
		        }
		        if (annotation.paramMap["knownVariables"] != null)
		        {
			        tooltip = "ToolTip=\"Known variables: ${annotation.paramMap["knownVariables"]}\""
		        }
	        }
	        builder.appendlnFix(indentation, """<Data Name="$dataName" SkipIfDefault="False" Default="$default" $tooltip $visibleIfStr meta:RefKey="String" />""")
        }
		else if (type == "XmlData")
	    {
		    val annotation = annotations.first { it.name == "DataXml" }
		    val actualClass = annotation.paramMap["actualClass"]!!

		    val classDef = classRegister.getClass(actualClass, classDefinition) ?: throw RuntimeException("createDefEntry: Unknown type '$actualClass'!")
		    if (classDef.isAbstract)
		    {
			    builder.appendlnFix(indentation, """<Data Name="$dataName" DefKey="${classDef.classDef!!.dataClassName}Defs" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
		    }
		    else
		    {
			    builder.appendlnFix(indentation, """<Data Name="$dataName" Keys="${classDef.classDef!!.dataClassName}" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
		    }
	    }
		else if (classRegister.getEnum(type, classDefinition) != null)
		{
			val enumDef = classRegister.getEnum(type, classDefinition)!!
            val enumVals = enumDef.getAsString()

			val defaultValue = if (type != this.type) "" else this.defaultValue
            var defaultStr = ""
            if (defaultValue.isNotBlank())
            {
                defaultStr = "Default=\"${defaultValue.split('.').last().toLowerCase().capitalize()}\""
            }

            builder.appendlnFix(indentation, """<Data Name="$dataName" EnumValues="$enumVals" $defaultStr $skipIfDefault $visibleIfStr meta:RefKey="Enum" />""")
		}
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)

			val dataArrayAnnotation = annotations.firstOrNull { it.name == "DataArray" }
			val minCount = dataArrayAnnotation?.paramMap?.get("minCount")
			val maxCount = dataArrayAnnotation?.paramMap?.get("maxCount")
			val minCountStr = if (minCount != null) "MinCount=\"$minCount\"" else ""
			val maxCountStr = if (maxCount != null) "MaxCount=\"$maxCount\"" else ""

			val childName = if (dataName.endsWith('s')) dataName.dropLast(1) else arrayType
			val classDef = classRegister.getClass(arrayType, classDefinition)

			if (arrayType == "Point")
			{
				if (annotations.any { it.name == "DataAsciiGrid" })
				{
					builder.appendlnFix(indentation, """<Data Name="$dataName" Default="#" ElementPerLine="True" IsAsciiGrid="True" $visibleIfStr meta:RefKey="MultilineString"/>""")
				}
				else
				{
					throw RuntimeException("Not supported")
				}
			}
			else if (classDef?.classDef != null)
			{
				if (classDef.classDef == null) throw RuntimeException("Class had no loaded classdef ${classDef.name}")

				val timelineAnnotation = annotations.firstOrNull { it.name == "DataTimeline" }
				if (timelineAnnotation != null)
				{
					if (timelineAnnotation.paramMap["timelineGroup"] == "true")
					{
						builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
						if (classDef.isAbstract)
						{
							builder.appendlnFix(indentation+1, """<Data Name="Timeline" DefKey="${classDef.classDef!!.dataClassName}Defs" meta:RefKey="Timeline" />""")
						}
						else
						{
							builder.appendlnFix(indentation+1, """<Data Name="Timeline" Keys="${classDef.classDef!!.dataClassName}" meta:RefKey="Timeline" />""")
						}
						builder.appendlnFix(indentation, """</Data>""")
					}
					else
					{
						if (classDef.isAbstract)
						{
							builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $maxCountStr DefKey="${classDef.classDef!!.dataClassName}Defs" $visibleIfStr meta:RefKey="Timeline" />""")
						}
						else
						{
							builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $maxCountStr Keys="${classDef.classDef!!.dataClassName}" $visibleIfStr meta:RefKey="Timeline" />""")
						}
					}
				}
				else
				{
					val uniqueChildren = if (dataArrayAnnotation?.paramMap?.get("childrenAreUnique") != null) """ChildrenAreUnique="True" """ else ""

					if (classDef.isAbstract)
					{
						builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $uniqueChildren $maxCountStr DefKey="${classDef.classDef!!.dataClassName}Defs" $visibleIfStr meta:RefKey="Collection" />""")
					}
					else
					{
						builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $uniqueChildren $maxCountStr Keys="${classDef.classDef!!.dataClassName}" $visibleIfStr meta:RefKey="Collection" />""")
					}
				}
			}
			else
			{
				builder.appendlnFix(indentation, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
				createDefEntry(builder, indentation+1, classDefinition, classRegister, arrayType, VariableType.VAR, childName)
				builder.appendlnFix(indentation, """</Data>""")
			}
		}
		else if (type.startsWith("ObjectMap<"))
        {

        }
		else if (type.startsWith("FastEnumMap<"))
		{
			val rawTypeParams = type.replace("FastEnumMap<", "").dropLast(1).split(",")
			val enumType = rawTypeParams[0].trim()
			val valueType = rawTypeParams[1].trim()

			val enumDef = classRegister.getEnum(enumType, classDefinition) ?: throw RuntimeException("Unable to find definition for enum for $type")

			builder.appendlnFix(indentation, """<Data Name="$dataName" $nullable $skipIfDefault $visibleIfStr  meta:RefKey="Struct">""")

			for (category in enumDef.values)
			{
				if (enumDef.values.size > 1)
				{
					builder.appendlnFix(indentation+1, """<!--${category.category}-->""")
				}

				for (value in category.values)
				{
					createDefEntry(builder, indentation+1, classDefinition, classRegister, valueType, VariableType.VAR, value.toLowerCase().capitalize())
				}
			}

			builder.appendlnFix(indentation, """</Data>""")
		}
        else
        {
            val classDef = classRegister.getClass(type, classDefinition) ?: throw RuntimeException("createDefEntry: Unknown type '$type'!")

	        val dataGraphAnnotation = annotations.firstOrNull { it.name == "DataGraphReference" }
	        if (dataGraphAnnotation != null)
	        {
		        val useParentDesc = if (dataGraphAnnotation.paramMap["useParentDescription"] != null) "UseParentDescription=\"True\"" else ""

		        if (classDef.isAbstract)
		        {
			        builder.appendlnFix(indentation, """<Data Name="$dataName" DefKey="${classDef.classDef!!.dataClassName}Defs" $useParentDesc $nullable $skipIfDefault $visibleIfStr meta:RefKey="GraphReference" />""")
		        }
		        else
		        {
			        builder.appendlnFix(indentation, """<Data Name="$dataName" Keys="${classDef.classDef!!.dataClassName}" $useParentDesc $nullable $skipIfDefault $visibleIfStr meta:RefKey="GraphReference" />""")
		        }
	        }
	        else
	        {
		        if (classDef.isAbstract)
		        {
			        builder.appendlnFix(indentation, """<Data Name="$dataName" DefKey="${classDef.classDef!!.dataClassName}Defs" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
		        }
		        else
		        {
			        builder.appendlnFix(indentation, """<Data Name="$dataName" Keys="${classDef.classDef!!.dataClassName}" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
		        }
	        }
        }
    }
}