package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.exp4j.Helpers.unescapeCharacters
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.XmlData
import java.util.*

class ConditionAction : AbstractMapGenerationAction()
{
	val conditions: Array<Condition> = Array<Condition>()

	//region non-data
	val variables = ObjectFloatMap<String>()
	//endregion

	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		for (cond in conditions)
		{
			var execute = false
			if (cond.condition == null)
			{
				execute = true
			}
			else
			{
				variables.clear()
				variables.putAll(args.variables)
				args.area.writeVariables(variables)

				val seed = generator.ran.nextLong()
				execute = cond.condition!!.evaluate(variables, seed) > 0
			}

			if (execute)
			{
				cond.child?.execute(generator, args)
				break
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val conditionsEl = xmlData.getChildByName("Conditions")
		if (conditionsEl != null)
		{
			for (el in conditionsEl.children)
			{
				val obj = Condition()
				obj.load(el)
				conditions.add(obj)
			}
		}
	}
	override val classID: String = "Condition"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		for (item in conditions)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}

class Condition : GraphXmlDataClass<MapGeneratorNode>()
{
	@DataCompiledExpression(createExpressionMethod = "createExpression")
	var condition: CompiledExpression? = null

	@DataGraphReference
	var child: MapGeneratorNode? = null

	fun createExpression(raw: String?): CompiledExpression?
	{
		if (raw == null || raw == "else")
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
		condition = createExpression(xmlData.get("Condition", null))
		childGUID = xmlData.get("Child", null)
	}
	var childGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		if (!childGUID.isNullOrBlank()) child = nodes[childGUID]!!
	}
	//endregion
}