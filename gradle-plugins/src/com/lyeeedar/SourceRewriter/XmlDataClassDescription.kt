package com.lyeeedar.build.SourceRewriter

class XmlDataClassDescription(val name: String, val defLine: String, val classIndentation: Int, val classDefinition: ClassDefinition, val classRegister: ClassRegister, val annotations: ArrayList<AnnotationDescription>)
{
    val variables = ArrayList<VariableDescription>()

	val classContents = ArrayList<String>()

	val dataClassName: String
	val dataClassCategory: String
	val forceGlobal: Boolean

    init
    {
        classDefinition.classDef = this

		val dataClassAnnotation = annotations.firstOrNull { it.name == "DataClass" }
		if (dataClassAnnotation != null)
		{
			dataClassName = dataClassAnnotation.paramMap["name"] ?: name.capitalize()
			dataClassCategory = dataClassAnnotation.paramMap["category"] ?: ""
			forceGlobal = dataClassAnnotation.paramMap["global"] == "true"
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
        builder.appendln(classIndentation+1, "//[generated]")

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
	    builder.appendln(classIndentation+1, "//[/generated]")

        builder.appendln(classIndentation, "}")
    }

    fun createDefFile(builder: IndentedStringBuilder, needsGlobalScope: Boolean)
    {
		val extends = if (classDefinition.superClass?.superClass != null) "Extends=\"${classDefinition.superClass!!.classDef!!.dataClassName}\"" else ""

        val dataFileAnnotation = annotations.firstOrNull { it.name == "DataFile" }
        if (dataFileAnnotation != null)
        {
            builder.appendlnFix(1, """<Definition Name="$dataClassName" Nullable="False" $extends meta:RefKey="Struct">""")
        }
        else
        {
            val global = if (needsGlobalScope || forceGlobal) "IsGlobal=\"True\"" else ""
            builder.appendlnFix(1, """<Definition Name="$dataClassName" Nullable="False" $global $extends meta:RefKey="StructDef">""")
        }

        for (variable in variables)
        {
			if (variable.raw.startsWith("abstract")) continue
            variable.createDefEntry(builder, classDefinition, classRegister)
        }

        builder.appendln(1, "</Definition>")
    }
}