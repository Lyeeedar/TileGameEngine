package com.lyeeedar.Systems

import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Sprite.DirectionalSprite

class DirectionalSpriteSystem(world: World): AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.DirectionalSprite).get())
{
	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val pos = entity.pos()!!
		val dirSprite = entity.directionalSprite()!!

		if (!dirSprite.data.directionalSprite.hasAnim(dirSprite.currentAnim))
		{
			dirSprite.currentAnim = "idle"
		}

		var renderable = entity.renderable()
		if (renderable == null)
		{
			renderable = entity.addComponent(ComponentType.Renderable) as RenderableComponent
			renderable.renderable = dirSprite.data.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)
		}

		if (pos.facing == Direction.SOUTH)
		{
			dirSprite.lastV = DirectionalSprite.VDir.DOWN
		}
		else if (pos.facing == Direction.NORTH)
		{
			dirSprite.lastV = DirectionalSprite.VDir.UP
		}
		else if (pos.facing == Direction.EAST)
		{
			dirSprite.lastH = DirectionalSprite.HDir.RIGHT
		}
		else if (pos.facing == Direction.WEST)
		{
			dirSprite.lastH = DirectionalSprite.HDir.LEFT
		}

		val chosen = dirSprite.data.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)

		if (chosen != renderable.renderable)
		{
			chosen.animation = renderable.renderable.animation
			renderable.renderable.animation = null
			renderable.renderable = chosen
		}
	}

	override fun onTurnEntity(entity: Entity)
	{

	}
}