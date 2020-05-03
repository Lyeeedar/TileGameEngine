package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Area", colour = "128,100,227")
class TranslateAction : AbstractMapGenerationAction()
{
	enum class Mode
	{
		RELATIVE,
		ABSOLUTE
	}

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var xEqn: CompiledExpression
	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var yEqn: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "%size")
		return CompiledExpression(cond)
	}

	lateinit var mode: Mode

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)
		val seedX = generator.ran.nextLong()
		val seedY = generator.ran.nextLong()

		val x = xEqn.evaluate(variables, seedX).floor()
		val y = yEqn.evaluate(variables, seedY).floor()

		if (mode == Mode.RELATIVE)
		{
			args.area.x += x
			args.area.y += y

			if (args.area.isPoints)
			{
				args.area.points.forEachIndexed { i, pos -> args.area.points[i] = Pos(pos.x + x, pos.y + y) }
			}
		}
		else if (mode == Mode.ABSOLUTE)
		{
			val dx = x - args.area.x
			val dy = y - args.area.y

			args.area.x += dx
			args.area.y += dy

			if (args.area.isPoints)
			{
				args.area.points.forEachIndexed { i, pos -> args.area.points[i] = Pos(pos.x + dx, pos.y + dy) }
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		xEqn = createExpression(xmlData.get("XEqn"))
		yEqn = createExpression(xmlData.get("YEqn"))
		mode = Mode.valueOf(xmlData.get("Mode").toUpperCase(Locale.ENGLISH))
	}
	override val classID: String = "Translate"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}