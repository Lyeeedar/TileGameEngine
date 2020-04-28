package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

@DataClass(category = "Permute", colour = "252,102,9")
class LockEntityTargetsAction : AbstractOneShotActionSequenceAction()
{
	override fun enter(state: ActionSequenceState)
	{
		state.lockedEntityTargets.clear()
		for (target in state.targets)
		{
			val tile = state.world.grid.tryGet(target, null)
			if (tile != null)
			{
				for (slot in SpaceSlot.EntityValues)
				{
					val entity = tile.contents[slot] ?: continue
					state.lockedEntityTargets.add(entity)
				}
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "LockEntityTargets"
	//endregion
}