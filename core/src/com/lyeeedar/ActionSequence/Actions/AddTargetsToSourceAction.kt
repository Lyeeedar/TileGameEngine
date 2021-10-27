package com.lyeeedar.ActionSequence.Actions

import com.badlogic.gdx.utils.Array
import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Pathfinding.BresenhamLine
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute")
class AddTargetsToSourceAction : AbstractOneShotActionSequenceAction()
{
	init
	{
		permutesTargets = true
	}

	@Transient
	private val temp = Array<Point>()

	override fun enter(state: ActionSequenceState)
	{
		temp.clear()
		temp.addAll(state.targets)

		for (target in temp)
		{
			val line = BresenhamLine.line(target, state.sourcePoint, state.world.grid, self = state.source.get()) ?: continue
			for (newtarget in line)
			{
				if (newtarget != state.sourcePoint) state.targets.add(newtarget)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "AddTargetsToSource"
	//endregion
}