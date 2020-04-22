package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.pos
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute", colour = "242,154,33")
class SelectTilesAction : AbstractOneShotActionSequenceAction()
{
	@DataCompiledExpression(knownVariables = "count", default = "count")
	lateinit var coverage: CompiledExpression

	var radius: Int = 1
	var emptyOnly: Boolean = false

	//region non-data
	val tiles = Array<Tile>(false, 4)
	val variables = ObjectFloatMap<String>()
	//endregion

	override fun enter(state: ActionSequenceState): ActionState
	{
		state.targets.clear()
		state.lockedEntityTargets.clear()

		tiles.clear()

		val pos = state.source.pos()!!.position

		val xs = max(0, pos.x-radius)
		val xe = min(state.world.grid.width, pos.x+radius)
		val ys = max(0, pos.y-radius)
		val ye = min(state.world.grid.height, pos.y+radius)

		for (x in xs until xe)
		{
			for (y in ys until ye)
			{
				val tile = state.world.grid[x, y]
				if (emptyOnly)
				{
					if (tile.contents.size == 0)
					{
						tiles.add(tile)
					}
				}
				else
				{
					tiles.add(tile)
				}
			}
		}

		variables.clear()
		variables["count"] = tiles.size.toFloat()

		val numTiles = coverage.evaluate(variables, state.seed++).round()
		if (numTiles == tiles.size)
		{
			state.targets.addAll(tiles)
		}
		else
		{
			val rng = Random.obtainTS(state.seed++)
			for (i in 0 until numTiles)
			{
				if (tiles.size == 0) break

				val tile = tiles.removeRandom(rng)
				state.targets.add(tile)
			}
			rng.freeTS()
		}

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		coverage = CompiledExpression(xmlData.get("Coverage"), "count")
		radius = xmlData.getInt("Radius", 1)
		emptyOnly = xmlData.getBoolean("EmptyOnly", false)
	}
	override val classID: String = "SelectTiles"
	//endregion
}