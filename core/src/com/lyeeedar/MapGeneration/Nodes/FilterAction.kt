package com.lyeeedar.MapGeneration.Nodes

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.MapGeneration.IMapGeneratorSymbol
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.DataValue
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set
import ktx.collections.toGdxArray

@DataClass(category = "Point", colour = "193,49,199")
class FilterAction : AbstractMapGenerationAction()
{
	enum class Mode
	{
		EMPTY,
		CHARACTER,
		CORNER,
		EDGE,
		CENTER
	}

	lateinit var mode: Mode

	@DataValue(visibleIf = "Mode == Character")
	var char: Char = ' '

	@DataValue(visibleIf = "Mode == Center")
	var centerDist: Int = 2

	@DataGraphReference
	var node: MapGeneratorNode? = null

	@DataGraphReference
	var remainder: MapGeneratorNode? = null

	val tempArray = com.badlogic.gdx.utils.Array<Pos>()
	override fun execute(generator: MapGenerator, args: NodeArguments)
	{
		val newArea = args.area.copy()
		if (!newArea.isPoints) newArea.convertToPoints()

		val remainderArea = if (remainder == null) null else newArea.copy()
		remainderArea?.points?.clear()

		tempArray.clear()
		tempArray.addAll(newArea.points)

		val condition: (symbol: IMapGeneratorSymbol, pos: Pos) -> Boolean = when (mode)
		{
			Mode.EMPTY -> fun (symbol, pos) = symbol.isEmpty()
			Mode.CHARACTER -> fun (symbol, pos) = symbol.char == char
			Mode.CORNER -> fun (symbol, pos): Boolean {
				if (!symbol.getPassable(SpaceSlot.ENTITY, null)) return false // cant be a corner if this isnt a floor

				// corner if any 2 sequential cardinal sides are not floors
				val dirs = Direction.CardinalValues.toGdxArray()
				dirs.add(dirs[0]) // wrap the end

				var isWall = false
				for (dir in dirs)
				{
					val symbol = newArea[newArea.x + pos.x + dir.x, newArea.y + pos.y + dir.y]
					if (symbol == null || !symbol.getPassable(SpaceSlot.ENTITY, null))
					{
						if (isWall)
						{
							return true
						}
						else
						{
							isWall = true
						}
					}
					else
					{
						isWall = false
					}
				}

				return false
			}
			Mode.EDGE -> fun (symbol, pos): Boolean {
				if (!symbol.getPassable(SpaceSlot.ENTITY, null)) return false // cant be a edge if this isnt a floor

				for (dir in Direction.CardinalValues)
				{
					val symbol = newArea[newArea.x + pos.x + dir.x, newArea.y + pos.y + dir.y]
					if (symbol == null || !symbol.getPassable(SpaceSlot.ENTITY, null))
					{
						return true
					}
				}

				return false
			}
			Mode.CENTER -> fun (symbol, pos): Boolean {
				if (!symbol.getPassable(SpaceSlot.ENTITY, null)) return false // cant be a center if this isnt a floor

				for (dir in Direction.CardinalValues)
				{
					for (i in 1..centerDist)
					{
						val symbol = newArea[newArea.x + pos.x + dir.x * i, newArea.y + pos.y + dir.y * i]
						if (symbol == null || !symbol.getPassable(SpaceSlot.ENTITY, null))
						{
							return false
						}
					}
				}

				return true
			}
		}

		for (point in tempArray)
		{
			val symbol = newArea[point.x - newArea.x, point.y - newArea.y]

			if (symbol == null || !condition.invoke(symbol, point))
			{
				newArea.points.removeValue(point, true)
				remainderArea?.points?.add(point)
			}
		}

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
		mode = Mode.valueOf(xmlData.get("Mode").uppercase(Locale.ENGLISH))
		char = xmlData.get("Char", " ")!![0]
		centerDist = xmlData.getInt("CenterDist", 2)
		nodeGUID = xmlData.get("Node", null)
		remainderGUID = xmlData.get("Remainder", null)
	}
	override val classID: String = "Filter"
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