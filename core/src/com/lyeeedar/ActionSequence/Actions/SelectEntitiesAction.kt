package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectSet
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.isAllies
import com.lyeeedar.Components.isEnemies
import com.lyeeedar.Components.pos
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.toGdxArray
import squidpony.squidmath.LightRNG

@DataClass(category = "Permute", colour = "212,176,126")
class SelectEntitiesAction : AbstractOneShotActionSequenceAction()
{
	enum class Mode
	{
		Allies,
		Enemies,
		Any
	}
	lateinit var mode: Mode

	@DataCompiledExpression(knownVariables = "count", default = "count")
	lateinit var count: CompiledExpression

	@DataCompiledExpression(knownVariables = "random,dist,hp", default = "random")
	lateinit var condition: CompiledExpression

	var radius: Int = 5
	var minimum: Boolean = true

	var allowSelf: Boolean = true
	var allowCurrent: Boolean = true

	//region non-data
	val oldTargetsStore = ObjectSet<Point>()
	val entities = ObjectSet<Entity>()
	val variables = ObjectFloatMap<String>()
	//endregion

	private fun createVariables(entity: Entity, state: ActionSequenceState, rng: LightRNG): ObjectFloatMap<String>
	{
		variables.clear()
		variables["random"] = rng.nextFloat()
		variables["dist"] = entity.pos()!!.position.taxiDist(state.source.pos()!!.position).toFloat()

		return variables
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		oldTargetsStore.clear()
		oldTargetsStore.addAll(state.targets)

		state.targets.clear()
		state.lockedEntityTargets.clear()

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
				if (!allowCurrent)
				{
					if (oldTargetsStore.contains(tile))
					{
						continue
					}
				}

				for (slot in SpaceSlot.EntityValues)
				{
					val entity = tile.contents[slot] ?: continue

					if (!allowSelf)
					{
						if (entity == state.source)
						{
							continue
						}
					}

					if (mode == Mode.Allies)
					{
						if (entity.isAllies(state.source))
						{
							entities.add(entity)
						}
					}
					else if (mode == Mode.Enemies)
					{
						if (entity.isEnemies(state.source))
						{
							entities.add(entity)
						}
					}
					else
					{
						entities.add(entity)
					}
				}
			}
		}

		val rng = Random.obtainTS(state.seed++)
		val sorted = if (minimum)
									entities.sortedBy { condition.evaluate(createVariables(it, state, rng), state.seed++) }.toGdxArray()
								else
									entities.sortedByDescending { condition.evaluate(createVariables(it, state, rng), state.seed++) }.toGdxArray()
		rng.freeTS()

		variables.clear()
		variables["count"] = sorted.size.toFloat()

		val numTiles = count.evaluate(variables, state.seed++).round()
		for (i in 0 until numTiles)
		{
			if (i == sorted.size) break

			val entity = sorted[i]
			state.targets.add(entity.pos()!!.position)
		}

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		mode = Mode.valueOf(xmlData.get("Mode").toUpperCase(Locale.ENGLISH))
		count = CompiledExpression(xmlData.get("Count"), "count")
		condition = CompiledExpression(xmlData.get("Condition"), "random,dist,hp")
		radius = xmlData.getInt("Radius", 5)
		minimum = xmlData.getBoolean("Minimum", true)
		allowSelf = xmlData.getBoolean("AllowSelf", true)
		allowCurrent = xmlData.getBoolean("AllowCurrent", true)
	}
	override val classID: String = "SelectEntities"
	//endregion
}