package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.exp4j.Helpers.unescapeCharacters
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Area", colour = "80,170,230")
class DivideAction : AbstractMapGenerationAction()
{
	val divisions: Array<Division> = Array<Division>()
	var onX: Boolean = true

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		args.area.xMode = onX

		var current = 0
		for (division in divisions)
		{
			// setup variables
			variables.clear()
			variables.putAll(args.variables)
			args.area.writeVariables(variables)
			val seed = generator.ran.nextLong()

			// evaluate size
			val size = if (division.size == null) args.area.size - current else Math.min(args.area.size - current, division.size!!.evaluate(variables, seed).floor())

			if (division.child != null && size > 0)
			{
				val newArea = args.area.copy()
				newArea.pos = args.area.pos + current
				newArea.size = size
				newArea.points.clear()
				newArea.addPointsWithin(args.area)

				if (newArea.hasContents)
				{
					val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
					division.child!!.execute(generator, newArgs)
				}
			}

			current += size
			if (current == args.area.size) break
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val divisionsEl = xmlData.getChildByName("Divisions")
		if (divisionsEl != null)
		{
			for (el in divisionsEl.children)
			{
				val objdivisions: Division
				val objdivisionsEl = xmlData.getChildByName("Divisions")!!
				objdivisions = Division()
				objdivisions.load(objdivisionsEl)
				divisions.add(objdivisions)
			}
		}
		onX = xmlData.getBoolean("OnX", true)
	}
	override val classID: String = "Divide"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		for (item in divisions)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}

class Division : GraphXmlDataClass<MapGeneratorNode>()
{
	@DataCompiledExpression(createExpressionMethod = "createExpression")
	var size: CompiledExpression? = null

	@DataGraphReference(useParentDescription = true)
	var child: MapGeneratorNode? = null

	fun createExpression(raw: String?): CompiledExpression?
	{
		if (raw == null || raw == "remainder")
		{
			return null
		}
		else
		{
			val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "#size").unescapeCharacters()
			return CompiledExpression(cond, Area.defaultVariables)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		size = createExpression(xmlData.get("Size", null))
		childGUID = xmlData.get("Child", null)
	}
	var childGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		if (!childGUID.isNullOrBlank()) child = nodes[childGUID]!!
	}
	//endregion
}