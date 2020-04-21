package com.lyeeedar.build.SourceRewriter

class XmlDataClassDescription(val name: String, val defLine: String, val classIndentation: Int, val classDefinition: ClassDefinition, val classRegister: ClassRegister, val annotations: ArrayList<AnnotationDescription>)
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

		val dataClassAnnotation = annotations.firstOrNull { it.name == "DataClass" }
		if (dataClassAnnotation != null)
		{
			dataClassName = dataClassAnnotation.paramMap["name"] ?: name.capitalize()
			dataClassCategory = dataClassAnnotation.paramMap["category"] ?: ""
			forceGlobal = dataClassAnnotation.paramMap["global"] == "true"
			colour = dataClassAnnotation.paramMap["colour"]
		}
		else
		{
			dataClassName = name.capitalize()
			dataClassCategory = ""
			forceGlobal = false
		}
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

        if (classDefinition.isAbstract)
        {
            for (childClass in classDefinition.inheritingClasses)
            {
                if (!childClass.isAbstract)
                {
                    if (childClass.namespace != classDefinition.namespace)
                    {
                        imports.add(childClass.namespace + ".${childClass.name}")
                    }
                }
            }
        }

	    val graphNodeType = getGraphNodeType()
	    if (graphNodeType != null)
	    {
		    imports.add("import com.badlogic.gdx.utils.ObjectMap")

		    val graphNodeClass = classRegister.getClass(graphNodeType.first, graphNodeType.second)!!
		    imports.add("import " + graphNodeClass.fullName)
	    }
    }

    fun write(builder: IndentedStringBuilder)
    {
        for (annotation in annotations)
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
            builder.appendln("")

            // write switch loader
            builder.appendln(classIndentation+1, "companion object")
            builder.appendln(classIndentation+1, "{")

            builder.appendln(classIndentation+2, "fun loadPolymorphicClass(classID: String): $name")
            builder.appendln(classIndentation+2, "{")

            builder.appendln(classIndentation+3, "return when (classID)")
            builder.appendln(classIndentation+3, "{")

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

                    builder.appendln(classIndentation+4, "$id -> ${childClass.name}()")
                }
            }

            builder.appendln(classIndentation+4, "else -> throw RuntimeException(\"Unknown classID '\$classID' for $name!\")")
            builder.appendln(classIndentation+3, "}")

            builder.appendln(classIndentation+2, "}")

            builder.appendln(classIndentation+1, "}")
        }
	    builder.appendln(classIndentation+1, "//endregion")

        builder.appendln(classIndentation, "}")
    }

    fun createDefFile(builder: IndentedStringBuilder, needsGlobalScope: Boolean)
    {
		val extends = if (classDefinition.superClass?.classDef?.name?.endsWith("XmlDataClass") == false) "Extends=\"${classDefinition.superClass!!.classDef!!.dataClassName}\"" else ""

	    val nodeMapVariable = variables.firstOrNull { it.annotations.any { it.name == "DataGraphNodes" } }

	    val colour = if (this.colour != null) """TextColour="$colour" """ else ""
	    val global = if (needsGlobalScope || forceGlobal) "IsGlobal=\"True\"" else ""

	    val collectionAnnotation = annotations.firstOrNull { it.name == "DataClassCollection" }
	    if (collectionAnnotation != null)
	    {
		    if (variables.size > 1) throw RuntimeException("DataClassAnnotation only works with a single child")

		    val dataGraphNode = annotations.firstOrNull { it.name == "DataGraphNode" }
		    val type = if (dataGraphNode != null) "GraphCollectionDef" else "CollectionDef"

		    val variable = variables[0]
		    val annotations = variable.annotations

		    val arrayType = variable.type.replace("Array<", "").dropLast(1)

		    val dataArrayAnnotation = annotations.firstOrNull { it.name == "DataArray" }
		    val minCount = dataArrayAnnotation?.paramMap?.get("minCount")
		    val maxCount = dataArrayAnnotation?.paramMap?.get("maxCount")
		    val minCountStr = if (minCount != null) "MinCount=\"$minCount\"" else ""
		    val maxCountStr = if (maxCount != null) "MaxCount=\"$maxCount\"" else ""

		    val classDef = classRegister.getClass(arrayType, classDefinition) ?: throw RuntimeException("createDefEntry: Unknown type '$arrayType' for '$type'!")

		    val def = if (classDef.isAbstract) """DefKey="${classDef.classDef!!.dataClassName}Defs" """  else """Keys="${classDef.classDef!!.dataClassName}" """

		    builder.appendlnFix(1, """<Definition Name="$dataClassName" $minCountStr $maxCountStr $def $colour $global meta:RefKey="$type">""")
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
			    variable.createDefEntry(builder, classDefinition, classRegister)
		    }
	    }

        builder.appendln(1, "</Definition>")
    }
}