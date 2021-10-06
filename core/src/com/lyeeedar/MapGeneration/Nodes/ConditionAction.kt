package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.*
import java.util.*

@DataClass(category = "Other", colour = "252,248,131")
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

				execute = cond.condition!!.evaluate(variables, rng) > 0
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
				val objconditions: Condition
				val objconditionsEl = el
				objconditions = Condition()
				objconditions.load(objconditionsEl)
				conditions.add(objconditions)
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
			val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "%size")
			return CompiledExpression(cond)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		condition = createExpression(xmlData.get("Condition", null))
		childGUID = xmlData.get("Child", null)
	}
	private var childGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		if (!childGUID.isNullOrBlank()) child = nodes[childGUID]!!
	}
	//endregion
}