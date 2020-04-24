package com.lyeeedar.AI.Tasks

import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import squidpony.squidmath.LightRNG

abstract class AbstractTask
{
	abstract fun execute(e: Entity, world: World, rng: LightRNG)
	abstract fun free()
}