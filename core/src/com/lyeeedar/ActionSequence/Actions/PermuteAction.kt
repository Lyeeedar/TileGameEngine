package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.DataAsciiGrid
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.toHitPointArray
import kotlin.math.roundToInt

@DataClass(category = "Permute", colour = "247,176,78,47")
class PermuteAction() : AbstractActionSequenceAction()
{
	var appendTargets: Boolean = false

	@DataAsciiGrid
	val hitPoints: Array<Point> = Array<Point>(4)

	//region non-data
	val mat = Matrix3()
	val vec = Vector3()
	val addedset = ObjectSet<Point>(4)
	val currentTargets = Array<Point>(false, 4)
	//endregion

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		currentTargets.clear()
		currentTargets.addAll(state.targets)

		if (!appendTargets)
		{
			state.targets.clear()
		}
		state.lockedEntityTargets.clear()

		mat.setToRotation(state.facing.angle)

		addedset.clear()
		for (tile in currentTargets)
		{
			for (point in hitPoints)
			{
				vec.set(point.x.toFloat(), point.y.toFloat(), 0f)
				vec.mul(mat)

				val dx = vec.x.roundToInt()
				val dy = vec.y.roundToInt()

				val ntile = Point(tile.x + dx, tile.y + dy)
				if (!addedset.contains(ntile))
				{
					state.targets.add(ntile)
					addedset.add(ntile)
				}
			}
		}

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		appendTargets = xmlData.getBoolean("AppendTargets", false)
		val hitPointsEl = xmlData.getChildByName("HitPoints")
		if (hitPointsEl != null) hitPoints.addAll(hitPointsEl.toHitPointArray())
		else hitPoints.add(Point(0, 0))
	}
	override val classID: String = "Permute"
	//endregion
}