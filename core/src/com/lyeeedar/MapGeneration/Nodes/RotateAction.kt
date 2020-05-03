package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Area", colour = "135,131,245")
class RotateAction : AbstractMapGenerationAction()
{
	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var degrees: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "%size")
		return CompiledExpression(cond)
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