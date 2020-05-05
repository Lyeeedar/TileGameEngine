package com.lyeeedar.Systems

import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.bakedLight
import com.lyeeedar.Components.position
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.set

class BakedLightingSystem(world: World<*>) : AbstractSystem(world)
{
	val lightingEntities = world.getEntitiesFor().all(ComponentType.Position, ComponentType.BakedLight).get()

	val lastEntities = ObjectSet<Entity>()

	val temp = Colour()

	var doneOnce = false

	override fun doUpdate(deltaTime: Float)
	{
		var recalculate = false
		if (!doneOnce)
		{
			recalculate = true
			doneOnce = true
		}

		if (lastEntities.size != lightingEntities.entities.size) recalculate = true

		if (!recalculate)
		{
			for (entity in lightingEntities.entities)
			{
				if (!lastEntities.contains(entity))
				{
					recalculate = true
					break
				}
			}
		}

		if (recalculate)
		{
			lastEntities.clear()
			lastEntities.addAll(lightingEntities.entities)

			for (tile in world.grid)
			{
				tile.bakedLighting.set(world.ambientLight)
			}

			for (entity in lightingEntities.entities)
			{
				val light = entity.bakedLight()!!
				val pos = entity.position()!!
				light.light.pos.set(pos.position)

				val points = light.light.getLightPoints()
				for (point in points)
				{
					val tile = world.grid.tryGet(point, null) ?: continue

					val dst2 = tile.euclideanDist2(pos.position)

					var alpha = 1f - dst2 / ( light.light.range * light.light.range )
					if (alpha < 0.001f) alpha = 0f

					temp.set(light.light.colour)
					temp *= alpha * light.light.brightness
					temp.a = 0f

					tile.bakedLighting += temp
					tile.isTileDirty = true
				}
			}
		}
	}
}