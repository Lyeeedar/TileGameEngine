package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import java.util.*
import ktx.collections.set

@DataClass(category = "Meta")
class SetTargetDelayAction : AbstractOneShotActionSequenceAction()
{
	enum class Mode
	{
		FROM_SOURCE,
		FROM_CENTER,
		RANDOM
	}

	lateinit var mode: Mode
	var maxDelay: Float = 0.5f

	override fun enter(state: ActionSequenceState)
	{
		if (state.targets.size == 0) return

		val min = state.targets.minByOrNull(Point::hashCode)!!
		val max = state.targets.maxByOrNull(Point::hashCode)!!

		val furthest = state.targets.maxByOrNull{ it.dist(state.sourcePoint) }!!

		val targetDelayMap = ObjectFloatMap<Point>()
		for (target in state.targets)
		{
			val delay = when(mode)
			{
				Mode.RANDOM -> {
					val alpha = Random.random(state.rng)
					maxDelay * alpha
				}
				Mode.FROM_SOURCE -> {
					val maxDist = furthest.euclideanDist(state.sourcePoint)
					val dist = target.euclideanDist(state.sourcePoint)
					val alpha = dist / maxDist
					maxDelay * alpha
				}
				Mode.FROM_CENTER -> {
					val center = min.lerp(max, 0.5f)
					val maxDist = center.euclideanDist(max)
					val dist = center.euclideanDist(target)
					val alpha = dist / maxDist
					maxDelay * alpha
				}
			}
			targetDelayMap[target] = delay
		}
		state.data[targetDelayKey] = targetDelayMap
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		mode = Mode.valueOf(xmlData.get("Mode").uppercase(Locale.ENGLISH))
		maxDelay = xmlData.getFloat("MaxDelay", 0.5f)
	}
	override val classID: String = "SetTargetDelay"
	//endregion
}