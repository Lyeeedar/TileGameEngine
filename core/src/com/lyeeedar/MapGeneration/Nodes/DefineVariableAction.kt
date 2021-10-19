package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set

@DataClass(category = "Setup", colour = "255,246,20")
class DefineVariableAction : AbstractMapGenerationAction()
{
	lateinit var key: String

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var valueExp: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "%size")
		return CompiledExpression(cond)
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

		val value = valueExp.evaluate(variables, rng)

		args.variables[key] = value
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		key = xmlData.get("Key")
		valueExp = createExpression(xmlData.get("ValueExp", "1")!!)
	}
	override val classID: String = "DefineVariable"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}