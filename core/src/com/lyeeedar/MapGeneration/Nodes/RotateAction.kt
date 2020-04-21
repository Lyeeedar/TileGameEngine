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
import java.util.*

class RotateAction : AbstractMapGenerationAction()
{
	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var degrees: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "#size").unescapeCharacters()
		return CompiledExpression(cond, Area.defaultVariables)
	}

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seed = generator.ran.nextLong()

		val angle = degrees.evaluate(variables, seed)
		args.area.orientation += angle
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		degrees = createExpression(xmlData.get("Degrees"))
	}
	override val classID: String = "Rotate"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}