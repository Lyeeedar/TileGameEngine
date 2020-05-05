package com.lyeeedar.Systems

import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Sprite.DirectionalSprite
import com.lyeeedar.Renderables.Sprite.Sprite

class DirectionalSpriteSystem(world: World<*>): AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.DirectionalSprite).get())
{
	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val pos = entity.position()!!
		val dirSprite = entity.directionalSprite()!!

		if (!dirSprite.directionalSprite.hasAnim(dirSprite.currentAnim))
		{
			dirSprite.currentAnim = "idle"
		}

		var renderable = entity.renderable()
		if (renderable == null)
		{
			renderable = entity.addComponent(ComponentType.Renderable) as RenderableComponent
			renderable.renderable = dirSprite.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)
		}
		val sprite = renderable.renderable as Sprite

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

		val chosen = dirSprite.directionalSprite.getSprite(dirSprite.currentAnim, dirSprite.lastV, dirSprite.lastH)

		if (chosen != sprite)
		{
			chosen.animation = sprite.animation
			chosen.removeAmount = sprite.removeAmount
			sprite.animation = null

			renderable.renderable = chosen
		}
	}

	override fun onTurnEntity(entity: Entity)
	{

	}
}