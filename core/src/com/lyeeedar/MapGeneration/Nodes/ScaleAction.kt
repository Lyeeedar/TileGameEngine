package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Area", colour = "112,109,201")
class ScaleAction : AbstractMapGenerationAction()
{
	enum class Mode
	{
		ADDITIVE,
		MULTIPLICATIVE,
		ABSOLUTE
	}

	lateinit var mode: Mode

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var xEqn: CompiledExpression

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var yEqn: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "%size")
		return CompiledExpression(cond)
	}

	lateinit var snap: Direction

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)

		val oldWidth = args.area.width
		val oldHeight = args.area.height

		val x = xEqn.evaluate(variables, rng)
		val y = yEqn.evaluate(variables, rng)

		if (mode == Mode.ADDITIVE)
		{
			args.area.width += x.toInt()
			args.area.height += y.toInt()
		}
		else if (mode == Mode.MULTIPLICATIVE)
		{
			args.area.width = (args.area.width * x).toInt()
			args.area.height = (args.area.height * y).toInt()
		}
		else if (mode == Mode.ABSOLUTE)
		{
			args.area.width = x.toInt()
			args.area.height = y.toInt()
		}
		else throw Exception("Unhandled scale mode '$mode'!")

		val diffX = args.area.width - oldWidth
		val diffY = args.area.height - oldHeight

		if (snap.x == 0)
		{
			args.area.x -= diffX / 2
		}
		else if (snap.x < 0)
		{

		}
		else if (snap.x > 0)
		{
			args.area.x = (args.area.x + oldWidth) - args.area.width
		}

		if (snap.y == 0)
		{
			args.area.y -= diffY / 2
		}
		else if (snap.y < 0)
		{

		}
		else if (snap.y > 0)
		{
			args.area.y = (args.area.y + oldHeight) - args.area.height
		}

		if (args.area.isPoints)
		{
			for (point in args.area.points.toList())
			{
				if (point.x < args.area.x || point.y < args.area.y || point.x >= args.area.x+args.area.width || point.y >= args.area.y+args.area.height)
				{
					args.area.points.removeValue(point, true)
				}
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		mode = Mode.valueOf(xmlData.get("Mode").toUpperCase(Locale.ENGLISH))
		xEqn = createExpression(xmlData.get("XEqn"))
		yEqn = createExpression(xmlData.get("YEqn"))
		snap = Direction.valueOf(xmlData.get("Snap").toUpperCase(Locale.ENGLISH))
	}
	override val classID: String = "Scale"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}