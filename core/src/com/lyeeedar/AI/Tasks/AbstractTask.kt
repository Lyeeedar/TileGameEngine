package com.lyeeedar.AI.Tasks

import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World

abstract class AbstractTask
{
	abstract fun execute(e: Entity, world: World)
	abstract fun free()
}