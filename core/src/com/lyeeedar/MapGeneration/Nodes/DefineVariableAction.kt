package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.exp4j.Helpers.unescapeCharacters
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.set
import java.util.*

class DefineVariableAction : AbstractMapGenerationAction()
{
	lateinit var key: String

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var valueExp: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "#size").unescapeCharacters()
		return CompiledExpression(cond, Area.defaultVariables)
	}

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		when (key)
		{
			"size", "pos", "count", "x", "y", "width", "height" -> throw UnsupportedOperationException("Define is using reserved name '$key'!")
		}

		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)

		val seed = generator.ran.nextLong()
		val value = valueExp.evaluate(variables, seed)

		args.variables[key] = value
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		key = xmlData.get("Key")
		valueExp = createExpression(xmlData.get("ValueExp"))
	}
	override val classID: String = "DefineVariable"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}