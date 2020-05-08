package com.lyeeedar.build.SourceRewriter

import com.lyeeedar.SourceRewriter.colourFromStringHash

class XmlDataClassDescription(val name: String, val defLine: String, val classIndentation: Int, val classDefinition: ClassDefinition, val classRegister: ClassRegister, val annotationsRaw: ArrayList<AnnotationDescription>)
{
    val variables = ArrayList<VariableDescription>()

	val classContents = ArrayList<String>()

	val dataClassName: String
	val dataClassCategory: String
	val forceGlobal: Boolean
	var colour: String? = null

    init
    {
        classDefinition.classDef = this

	    classDefinition.generateClassID()
	    val classIDName = (classDefinition.classID ?: classDefinition.generatedClassID)?.replace("\"", "")

		val dataClassAnnotation = annotationsRaw.firstOrNull { it.name == "DataClass" }
		if (dataClassAnnotation != null)
		{
			dataClassName = dataClassAnnotation.paramMap["name"] ?: classIDName ?: name.capitalize()
			dataClassCategory = dataClassAnnotation.paramMap["category"] ?: ""
			forceGlobal = dataClassAnnotation.paramMap["global"] == "true"
			colour = dataClassAnnotation.paramMap["colour"]
		}
		else
		{
			dataClassName = classIDName ?: name.capitalize()
			dataClassCategory = ""
			forceGlobal = false
		}

	    if (colour == null)
	    {
		    colour = colourFromStringHash(name, 0.8f)
	    }
    }

	fun getAnnotations(): List<AnnotationDescription>
	{
		val dict = HashMap<String, AnnotationDescription>()

		val parentAnnotations = classDefinition.superClass?.classDef?.getAnnotations()
		if (parentAnnotations != null)
		{
			for (annotation in parentAnnotations)
			{
				dict[annotation.name] = annotation
			}
		}

		for (annotation in annotationsRaw)
		{
			dict[annotation.name] = annotation
		}

		return dict.values.toList()
	}

	fun getGraphNodeType(): Pair<String, ClassDefinition>?
	{
		var current = classDefinition
		while (current.superClass != null)
		{
			val superClass = current.superClass!!
			if (superClass.name == "GraphXmlDataClass")
			{
				return Pair(current.inheritDeclarations[0].split("<")[1].split(">")[0], current)
			}

			current = superClass
		}

		return null
	}

    fun resolveImports(imports: HashSet<String>)
    {
        for (variable in variables)
        {
            variable.resolveImports(imports, classDefinition, classRegister)
        }
	    imports.add("import com.lyeeedar.Util.XmlData")

	    if (classDefinition.isAbstract)
	    {
		    imports.add("import com.lyeeedar.Util.XmlDataClassLoader")
	    }

	    val graphNodeType = getGraphNodeType()
	    if (graphNodeType != null)
	    {
		    imports.add("import com.badlogic.gdx.utils.ObjectMap")

		    val graphNodeClass = classRegister.getClass(graphNodeType.first, graphNodeType.second)!!
		    imports.add("import " + graphNodeClass.fullName)
	    }
    }

    fun write(builder: IndentedStringBuilder, loaderBuilder: IndentedStringBuilder)
    {
        for (annotation in annotationsRaw)
        {
            builder.appendln(classIndentation, annotation.annotationString)
        }

        builder.appendln(classIndentation, defLine)
        builder.appendln(classIndentation, "{")

		// remove blank lines from end of content
		for (i in classContents.size-1 downTo 0)
		{
			if (classContents[i].isBlank()) classContents.removeAt(i)
			else break
		}

		for (line in classContents)
		{
			builder.appendln(line)
		}

	    builder.appendln("")
        builder.appendln(classIndentation+1, "//region generated")

        builder.appendln(classIndentation+1, "override fun load(xmlData: XmlData)")
        builder.appendln(classIndentation+1, "{")

	    // write load
        if (classDefinition.superClass != null && !classDefinition.superClass!!.name.endsWith("XmlDataClass"))
        {
            builder.appendln(classIndentation+2, "super.load(xmlData)")

	        classDefinition.referencedClasses.add(classDefinition.superClass!!)
        }

	    val extraVariables = ArrayList<String>()
	    if (classDefinition.isAbstract && classDefinition.superClass!!.name.endsWith("XmlDataClass"))
	    {
		    if (!variables.any { it.name == "classID" })
		    {
			    extraVariables.add("abstract val classID: String")
		    }
	    }
	    else
	    {
		    if (classDefinition.classID == null)
		    {
			    classDefinition.generateClassID()

			    if (classDefinition.generatedClassID != null)
			    {
				    extraVariables.add("override val classID: String = ${classDefinition.generatedClassID}")
			    }
		    }
	    }

        for (variable in variables)
        {
            variable.writeLoad(builder, classIndentation+2, classDefinition, classRegister, extraVariables)
        }
	    val nodeMapVariable = variables.firstOrNull { it.annotations.any { it.name == "DataGraphNodes" } }
	    if (nodeMapVariable != null)
	    {
		    builder.appendln(classIndentation+2, "resolve(${nodeMapVariable.name})")
	    }

	    if (classContents.any { it.contains("override fun afterLoad()") })
	    {
		    builder.appendln(classIndentation + 2, "afterLoad()")
	    }

        builder.appendln(classIndentation+1, "}")

	    for (line in extraVariables)
	    {
		    builder.appendln(classIndentation+1, line)
	    }

	    // write resolve
		val graphNodeType = getGraphNodeType()
	    if (graphNodeType != null)
	    {
		    builder.appendln(classIndentation + 1, "override fun resolve(nodes: ObjectMap<String, ${graphNodeType.first}>)")
		    builder.appendln(classIndentation + 1, "{")

		    if (classDefinition.superClass != null && !classDefinition.superClass!!.name.endsWith("XmlDataClass"))
		    {
			    builder.appendln(classIndentation + 2, "super.resolve(nodes)")
		    }

		    for (variable in variables)
		    {
			    variable.writeResolve(builder, classIndentation + 2, classDefinition, classRegister)
		    }
		    builder.appendln(classIndentation + 1, "}")
	    }

	    // write loadpolymorphic
        if (classDefinition.isAbstract)
        {
	        loaderBuilder.appendln(classIndentation+2, "fun load$name(classID: String): ${classDefinition.fullName}")
	        loaderBuilder.appendln(classIndentation+2, "{")

	        loaderBuilder.appendln(classIndentation+3, "return when (classID)")
	        loaderBuilder.appendln(classIndentation+3, "{")

            for (childClass in classDefinition.inheritingClasses)
            {
                if (!childClass.isAbstract)
                {
	                var id = childClass.classID
	                if (id == null)
	                {
						childClass.generateClassID()
		                id = childClass.generatedClassID
	                }

	                loaderBuilder.appendln(classIndentation+4, "$id -> ${childClass.fullName}()")
                }
            }

	        loaderBuilder.appendln(classIndentation+4, "else -> throw RuntimeException(\"Unknown classID '\$classID' for $name!\")")
	        loaderBuilder.appendln(classIndentation+3, "}")

	        loaderBuilder.appendln(classIndentation+2, "}")
        }
	    builder.appendln(classIndentation+1, "//endregion")

        builder.appendln(classIndentation, "}")
    }

    fun createDefFile(builder: IndentedStringBuilder, needsGlobalScope: Boolean)
    {
	    println("Writing contents for: " + name)

	    val annotations = getAnnotations()

		val extends = if (classDefinition.superClass?.classDef?.name?.endsWith("XmlDataClass") == false) "Extends=\"${classDefinition.superClass!!.classDef!!.dataClassName}\"" else ""

	    val nodeMapVariable = variables.firstOrNull { it.annotations.any { it.name == "DataGraphNodes" } }
	    val nodeRefVariable = variables.firstOrNull { it.annotations.any { it.name == "DataGraphReference" } }

	    var colour = """TextColour="$colour" """
	    val global = if (needsGlobalScope || forceGlobal) "IsGlobal=\"True\"" else ""

	    val dataGraphNode = annotations.firstOrNull { it.name == "DataGraphNode" }
	    if (dataGraphNode != null)
	    {
		    colour = """Background="${this.colour}" """
	    }

	    val collectionAnnotation = annotations.firstOrNull { it.name == "DataClassCollection" }
	    if (collectionAnnotation != null)
	    {
		    if (classDefinition.isAbstract) return

		    val type = if (dataGraphNode != null) "GraphCollectionDef" else "CollectionDef"

		    val collectionVariable = variables.firstOrNull { it.type.startsWith("Array<") } ?: throw RuntimeException("Unable to find collection container!")
		    val nonCollectionVariables = variables.filter { it != collectionVariable }.toList()

		    var hasAttributes = ""
		    if (nonCollectionVariables.size > 0)
		    {
			    hasAttributes = """HasAttributes="True" """
		    }

		    val annotations = collectionVariable.annotations

		    val arrayType = collectionVariable.type.replace("Array<", "").dropLast(1)

		    val dataArrayAnnotation = annotations.firstOrNull { it.name == "DataArray" }
		    val minCount = dataArrayAnnotation?.paramMap?.get("minCount")
		    val maxCount = dataArrayAnnotation?.paramMap?.get("maxCount")
		    val minCountStr = if (minCount != null) "MinCount=\"$minCount\"" else ""
		    val maxCountStr = if (maxCount != null) "MaxCount=\"$maxCount\"" else ""

		    val classDef = classRegister.getClass(arrayType, classDefinition)
		                   ?: throw RuntimeException("createDefEntry: Unknown type '$arrayType' for '$type'!")

		    val def = if (classDef.isAbstract) """DefKey="${classDef.classDef!!.dataClassName}Defs" """ else """Keys="${classDef.classDef!!.dataClassName}" """

		    builder.appendlnFix(1, """<Definition Name="$dataClassName" $minCountStr $maxCountStr $def $colour $hasAttributes $global meta:RefKey="$type">""")

		    if (nonCollectionVariables.size > 0)
		    {
			    builder.appendln(2, "<Attributes meta:RefKey=\"Attributes\">")

			    for (variable in nonCollectionVariables)
			    {
				    if (variable.raw.startsWith("abstract")) continue
				    variable.createDefEntry(3, builder, classDefinition, classRegister)
			    }
			    builder.appendln(2, "</Attributes>")
		    }
	    }
	    else
	    {
		    val dataFileAnnotation = annotations.firstOrNull { it.name == "DataFile" }
		    if (dataFileAnnotation != null)
		    {
			    if (nodeMapVariable != null)
			    {
				    builder.appendlnFix(1, """<Definition Name="$dataClassName" AllowCircularLinks="True" FlattenData="True" NodeStoreName="${nodeMapVariable.dataName}" Nullable="False" $colour $extends meta:RefKey="GraphStruct">""")
			    }
			    else if (nodeRefVariable != null)
			    {
				    builder.appendlnFix(1, """<Definition Name="$dataClassName" Nullable="False" $colour $extends meta:RefKey="GraphStruct">""")
			    }
			    else
			    {
				    builder.appendlnFix(1, """<Definition Name="$dataClassName" Nullable="False" $colour $extends meta:RefKey="Struct">""")
			    }
		    }
		    else
		    {
			    val dataGraphNode = annotations.firstOrNull { it.name == "DataGraphNode" }
			    val type = if (dataGraphNode != null) "GraphStructDef" else "StructDef"
			    builder.appendlnFix(1, """<Definition Name="$dataClassName" Nullable="False" $colour $global $extends meta:RefKey="$type">""")
		    }

		    if (classDefinition.generatedClassID != null)
		    {
			    builder.appendln(2, """<Const Name="classID">${classDefinition.generatedClassID!!.replace("\"", "")}</Const>""")
		    }
		    for (variable in variables)
		    {
			    if (variable.raw.startsWith("abstract")) continue
			    variable.createDefEntry(2, builder, classDefinition, classRegister)
		    }
	    }

        builder.appendln(1, "</Definition>")
    }
}