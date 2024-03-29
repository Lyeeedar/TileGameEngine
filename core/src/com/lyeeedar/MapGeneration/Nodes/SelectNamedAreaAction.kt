package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.MapGeneration.Area
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set

@DataClass(category = "Rooms", colour = "255,196,0")
class SelectNamedAreaAction : AbstractMapGenerationAction()
{
	enum class Mode
	{
		RANDOM,
		SMALLEST,
		LARGEST,
		CLOSEST,
		FURTHEST
	}

	lateinit var mode: Mode

	@DataCompiledExpression(createExpressionMethod = "createExpression")
	lateinit var countExp: CompiledExpression

	lateinit var name: String

	fun createExpression(raw: String): CompiledExpression
	{
		val cond = raw.lowercase(Locale.ENGLISH).replace("%", "%count")
		return CompiledExpression(cond)
	}

	@DataGraphReference
	var node: MapGeneratorNode? = null
	@DataGraphReference
	var remainder: MapGeneratorNode? = null

	val tempArray = Array<Area>(false, 8)
	val variables = com.badlogic.gdx.utils.ObjectFloatMap<String>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val areas = generator.namedAreas[name]!!

		variables.clear()
		variables.putAll(args.variables)
		variables["count"] = areas.size.toFloat()

		val count = countExp.evaluate(variables, rng).floor()

		tempArray.addAll(areas)

		when (mode)
		{
			Mode.RANDOM -> {
				for (i in 0 until count)
				{
					val area = tempArray.removeRandom(rng)
					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(generator, newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.SMALLEST -> {
				val sorted = tempArray.sortedBy { it.getAllPoints().size }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(generator, newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.LARGEST -> {
				val sorted = tempArray.sortedByDescending { it.getAllPoints().size }
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(generator, newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.CLOSEST -> {
				val sorted = tempArray.sortedBy {
					it.getAllPoints().minOf { ap ->
						args.area.getAllPoints().minOf { tp -> tp.dst2(ap) }
					}
				}
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(generator, newArgs)

					if (tempArray.size == 0) break
				}
			}
			Mode.FURTHEST -> {
				val sorted = tempArray.sortedByDescending {
					it.getAllPoints().minOf { ap ->
						args.area.getAllPoints().minOf { tp -> tp.dst2(ap) }
					}
				}
				for (i in 0 until count)
				{
					val area = sorted[i]
					tempArray.removeValue(area, true)

					val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)

					node?.execute(generator, newArgs)

					if (tempArray.size == 0) break
				}
			}
		}

		if (remainder != null)
		{
			for (area in tempArray)
			{
				val newArgs = NodeArguments(area.copy(), args.variables, args.symbolTable)
				remainder!!.execute(generator, newArgs)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		mode = Mode.valueOf(xmlData.get("Mode").uppercase(Locale.ENGLISH))
		countExp = createExpression(xmlData.get("CountExp", "1")!!)
		name = xmlData.get("Name")
		nodeGUID = xmlData.get("Node", null)
		remainderGUID = xmlData.get("Remainder", null)
	}
	override val classID: String = "SelectNamedArea"
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