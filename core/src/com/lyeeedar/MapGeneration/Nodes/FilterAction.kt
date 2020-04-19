package com.lyeeedar.MapGeneration.Nodes

import com.lyeeedar.Direction
import com.lyeeedar.MapGeneration.MapGenerator
import com.lyeeedar.MapGeneration.MapGeneratorNode
import com.lyeeedar.MapGeneration.Pos
import com.lyeeedar.MapGeneration.Symbol
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.XmlData
import ktx.collections.toGdxArray
import java.util.*

class FilterAction(generator: MapGenerator) : AbstractMapGenerationAction(generator)
{
	enum class Mode
	{
		NOCONTENT,
		CHARACTER,
		CORNER,
		EDGE,
		CENTER
	}

	lateinit var mode: Mode

	var char: Char = ' '
	var centerDist = 2

	lateinit var nodeGuid: String
	lateinit var remainderGuid: String

	var node: MapGeneratorNode? = null
	var remainder: MapGeneratorNode? = null

	val tempArray = com.badlogic.gdx.utils.Array<Pos>()
	override fun execute(args: NodeArguments)
	{
		val newArea = args.area.copy()
		if (!newArea.isPoints) newArea.convertToPoints()

		val remainderArea = if (remainder == null) null else newArea.copy()
		remainderArea?.points?.clear()

		tempArray.clear()
		tempArray.addAll(newArea.points)

		val condition: (symbol: Symbol, pos: Pos) -> Boolean = when (mode)
		{
			Mode.NOCONTENT -> fun (symbol, pos) = symbol.content == null
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
			node!!.execute(newArgs)
		}

		if (remainder != null && remainderArea!!.points.size > 0)
		{
			val newArgs = NodeArguments(remainderArea, args.variables, args.symbolTable)
			remainder!!.execute(newArgs)
		}
	}

	override fun parse(xmlData: XmlData)
	{
		mode = Mode.valueOf(xmlData.get("Mode", "Type")!!.toUpperCase(Locale.ENGLISH))

		char = xmlData.get("Character", " ")!![0]
		centerDist = xmlData.getInt("CenterDist", 2)

		nodeGuid = xmlData.get("Node", "")!!
		remainderGuid = xmlData.get("Remainder", "")!!
	}

	override fun resolve()
	{
		if (nodeGuid.isNotBlank()) node = generator.nodeMap[nodeGuid]
		if (remainderGuid.isNotBlank()) remainder = generator.nodeMap[remainderGuid]
	}
}