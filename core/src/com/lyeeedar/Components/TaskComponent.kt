package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.AI.Tasks.AbstractTask

class TaskComponent : NonDataComponent()
{
	override val type: ComponentType = ComponentType.Task

	val tasks = Array<AbstractTask>()
	var actionAccumulator = 0f

	override fun reset()
	{

	}
}