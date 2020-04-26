package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.exp4j.Helpers.unescapeCharacters
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Point", colour = "214,86,219")
class TakeAction : AbstractMapGenerationAction()
{
	enum class Mode
	{
		RANDOM
	}

	lateinit var mode: Mode

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var countExp: CompiledExpression
	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "#count").unescapeCharacters()
		return CompiledExpression(cond, Area.defaultVariables)
	}

	@DataGraphReference
	var node: MapGeneratorNode? = null
	@DataGraphReference
	var remainder: MapGeneratorNode? = null

	val variables = ObjectFloatMap<String>()
	val tempArray = Array<Pos>(false, 16)
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val newArea = args.area.copy()
		if (!newArea.isPoints) newArea.convertToPoints()

		if (newArea.points.size == 0) return

		val remainderArea = if (remainder == null) null else newArea.copy()
		remainderArea?.points?.clear()

		variables.clear()
		variables.putAll(args.variables)
		newArea.writeVariables(variables)
		val seed = generator.ran.nextLong()

		val count = countExp.evaluate(variables, seed).floor()

		tempArray.clear()
		tempArray.addAll(newArea.points)
		newArea.points.clear()

		if (mode == Mode.RANDOM)
		{
			for (i in 0 until count)
			{
				val point = tempArray.removeRandom(generator.ran)
				newArea.points.add(point)
			}
		}
		else
		{
			throw Exception("Unhandled take mode '$mode'!")
		}

		remainderArea?.points?.addAll(tempArray)

		if (node != null && newArea.points.size > 0)
		{
			val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
			node!!.execute(generator, newArgs)
		}

		if (remainder != null && remainderArea!!.points.size > 0)
		{
			val newArgs = NodeArguments(remainderArea, args.variables, args.symbolTable)
			remainder!!.execute(generator, newArgs)
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		mode = Mode.valueOf(xmlData.get("Mode").toUpperCase(Locale.ENGLISH))
		countExp = createExpression(xmlData.get("CountExp"))
		nodeGUID = xmlData.get("Node", null)
		remainderGUID = xmlData.get("Remainder", null)
		afterLoad()
	}
	override val classID: String = "Take"
	private var nodeGUID: String? = null
	private var remainderGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		if (!nodeGUID.isNullOrBlank()) node = nodes[nodeGUID]!!
		if (!remainderGUID.isNullOrBlank()) remainder = nodes[remainderGUID]!!
	}
	//endregion
}