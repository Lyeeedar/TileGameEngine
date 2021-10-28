package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set

@DataClass(category = "Area", colour = "81,154,245")
class RepeatAction : AbstractMapGenerationAction()
{
	enum class RemainderMode
	{
		NODE,
		PAD,
		EXPAND
	}

	lateinit var remainderMode: RemainderMode
	var onX: Boolean = true

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var size: CompiledExpression

	@DataGraphReference
	lateinit var child: MapGeneratorNode

	@DataValue(visibleIf = "RemainderMode == Node")
	@DataGraphReference
	var remainder: MapGeneratorNode? = null

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.lowercase(Locale.ENGLISH).replace("%", "%size")
		return CompiledExpression(cond)
	}

	val variables = ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		args.area.xMode = onX

		variables.clear()
		variables.putAll(args.variables)
		args.area.writeVariables(variables)

		val points = Array<RepeatDivision>()

		var current = 0
		val totalSize = args.area.size

		while (current < totalSize)
		{
			val size = size.evaluate(variables, rng).floor()

			if (current + size > totalSize) break

			points.add(RepeatDivision(current, size))

			current += size
		}

		val remaining = totalSize - current
		if (remaining > 0)
		{
			if (remainderMode == RemainderMode.PAD)
			{
				val paddingRaw = remaining.toFloat() / points.size.toFloat()
				val padding = paddingRaw.toInt()
				val paddingRemainder = paddingRaw - padding

				var accumulatedOffset = 0
				var accumulatedRemainder = 0f
				for (point in points)
				{
					point.pos += accumulatedOffset
					accumulatedOffset += padding

					accumulatedRemainder += paddingRemainder
					if (accumulatedRemainder > 1f)
					{
						accumulatedRemainder -= 1f

						accumulatedOffset += 1
					}
				}
			}
			else if (remainderMode == RemainderMode.EXPAND)
			{
				val paddingRaw = remaining.toFloat() / points.size.toFloat()
				val padding = paddingRaw.toInt()
				val paddingRemainder = paddingRaw - padding

				var accumulatedOffset = 0
				var accumulatedRemainder = 0f
				for (point in points)
				{
					point.pos += accumulatedOffset
					point.size += padding
					accumulatedOffset += padding

					accumulatedRemainder += paddingRemainder
					if (accumulatedRemainder > 1f)
					{
						accumulatedRemainder -= 1f

						point.size += 1
						accumulatedOffset += 1
					}
				}
			}
		}

		for (point in points)
		{
			val newArea = args.area.copy()
			newArea.pos = args.area.pos + point.pos
			newArea.size = point.size

			newArea.points.clear()
			newArea.addPointsWithin(args.area)

			if (newArea.hasContents)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				child.execute(generator, newArgs)
			}
		}

		if (remainderMode == RemainderMode.NODE && remainder != null && remaining > 0)
		{
			val newArea = args.area.copy()
			newArea.pos = args.area.pos + current
			newArea.size = remaining

			newArea.points.clear()
			newArea.addPointsWithin(args.area)

			if (newArea.hasContents)
			{
				val newArgs = NodeArguments(newArea, args.variables, args.symbolTable)
				remainder!!.execute(generator, newArgs)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		remainderMode = RemainderMode.valueOf(xmlData.get("RemainderMode").uppercase(Locale.ENGLISH))
		onX = xmlData.getBoolean("OnX", true)
		size = createExpression(xmlData.get("Size", "1")!!)
		childGUID = xmlData.get("Child")
		remainderGUID = xmlData.get("Remainder", null)
	}
	override val classID: String = "Repeat"
	private lateinit var childGUID: String
	private var remainderGUID: String? = null
	override fun resolve(nodes: ObjectMap<String, MapGeneratorNode>)
	{
		super.resolve(nodes)
		child = nodes[childGUID]!!
		if (!remainderGUID.isNullOrBlank()) remainder = nodes[remainderGUID]!!
	}
	//endregion
}

class RepeatDivision(var pos: Int, var size: Int)