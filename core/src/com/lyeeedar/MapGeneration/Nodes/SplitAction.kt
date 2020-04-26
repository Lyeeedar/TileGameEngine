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

enum class SplitSide
{
	NORTH,
	SOUTH,
	EAST,
	WEST,
	EDGE,
	REMAINDER
}

@DataClass(category = "Area", colour = "28,168,232")
class SplitAction : AbstractMapGenerationAction()
{
	val splits: Array<Split> = Array<Split>()

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		var currentArea = args.area.copy()
		for (i in 0 until splits.size)
		{
			val split = splits[i]

			val newArea = currentArea.copy()
			val nextArea = currentArea.copy()

			if (split.side == SplitSide.NORTH)
			{
				currentArea.xMode = false

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).floor()

				newArea.x = currentArea.x
				newArea.y = currentArea.y + currentArea.height - size
				newArea.width = currentArea.width
				newArea.height = size
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width
				nextArea.height = currentArea.height - size
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.SOUTH)
			{
				currentArea.xMode = false

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x
				newArea.y = currentArea.y
				newArea.width = currentArea.width
				newArea.height = size
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y + size
				nextArea.width = currentArea.width
				nextArea.height = currentArea.height - size
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.WEST)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x
				newArea.y = currentArea.y
				newArea.width = size
				newArea.height = currentArea.height
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x + size
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width - size
				nextArea.height = currentArea.height
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.EAST)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				newArea.x = currentArea.x + currentArea.width - size
				newArea.y = currentArea.y
				newArea.width = size
				newArea.height = currentArea.height
				newArea.points.clear()
				newArea.addPointsWithin(currentArea)

				nextArea.x = currentArea.x
				nextArea.y = currentArea.y
				nextArea.width = currentArea.width - size
				nextArea.height = currentArea.height
				nextArea.points.clear()
				nextArea.addPointsWithin(currentArea)
			}
			else if (split.side == SplitSide.EDGE)
			{
				currentArea.xMode = true

				variables.clear()
				variables.putAll(args.variables)
				currentArea.writeVariables(variables)
				val seed = generator.ran.nextLong()

				val size = split.size.evaluate(variables, seed).round()

				if (!newArea.isPoints)
				{
					newArea.points.clear()
					newArea.convertToPoints()
				}

				nextArea.x = currentArea.x + size
				nextArea.y = currentArea.y + size
				nextArea.width = currentArea.width - size * 2
				nextArea.height = currentArea.height - size * 2
				nextArea.points.clear()
				nextArea.addPointsWithin(newArea)

				for (point in nextArea.points) newArea.points.removeValue(point, true)
			}
			else if (split.side == SplitSide.REMAINDER)
			{
				if (split.child != null)
				{
					val newArgs = NodeArguments(currentArea, args.variables, args.symbolTable)
					split.child!!.execute(generator, newArgs)
				}

				break
			}
			else throw Exception("Unhandled split side '" + split.side + "'!")

			currentArea = nextArea

			if (split.child != null)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				split.child!!.execute(generator, newArgs)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val splitsEl = xmlData.getChildByName("Splits")
		if (splitsEl != null)
		{
			for (el in splitsEl.children)
			{
				val objsplits: Split
				val objsplitsEl = el
				objsplits = Split()
				objsplits.load(objsplitsEl)
				splits.add(objsplits)
			}
		}
		afterLoad()
	}
	override val classID: String = "Split"
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		for (item in splits)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}

class Split : GraphXmlDataClass<MapGeneratorNode>()
{
	lateinit var side: SplitSide

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var size: CompiledExpression

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.toLowerCase(Locale.ENGLISH).replace("%", "#size").unescapeCharacters()
		return CompiledExpression(cond, Area.defaultVariables)
	}

	@DataGraphReference
	var child: MapGeneratorNode? = null

	//region generated
	override fun load(xmlData: XmlData)
	{
		side = SplitSide.valueOf(xmlData.get("Side").toUpperCase(Locale.ENGLISH))
		size = createExpression(xmlData.get("Size"))
		childGUID = xmlData.get("Child", null)
		afterLoad()
	}
	var childGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		if (!childGUID.isNullOrBlank()) child = nodes[childGUID]!!
	}
	//endregion
}