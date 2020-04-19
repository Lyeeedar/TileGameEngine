package com.lyeeedar.Components

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Direction
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import java.util.*

inline fun Entity.pos(): PositionComponent? = this.components[ComponentType.Position] as PositionComponent?
class PositionComponent(): AbstractComponent()
{
	override val type: ComponentType = ComponentType.Position

	var position: Point = Point(-1, -1) // bottom left pos
		set(value)
		{
			if (value != field)
			{
				facing = Direction.getCardinalDirection(value.x - field.x, value.y - field.y)

				field = value
				max = value
				turnsOnTile = 0
			}
		}

	var min: Point
		set(value) { position = value }
		get() { return position }

	var offset: Vector2 = Vector2()

	var max: Point = Point(-1, -1)

	var size: Int = 1

	var slot: SpaceSlot = SpaceSlot.ENTITY

	var moveable = true

	var moveLocked = false

	var canFall = true

	var facing: Direction = Direction.SOUTH

	var turnsOnTile: Int = 0

	val x: Int
		get() = position.x
	val y: Int
		get() = position.y

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val slotEl = xml.get("SpaceSlot", null)
		if (slotEl != null) slot = SpaceSlot.valueOf(slotEl.toUpperCase(Locale.ENGLISH))

		canFall = xml.getBoolean("CanFall", true)
		moveable = xml.getBoolean("Moveable", true)

		size = xml.getInt("Size", 1)
		if (size != -1)
		{
			val renderable = entity.renderable()
			if (renderable != null)
			{
				renderable.renderable.size[0] = size
				renderable.renderable.size[1] = size
			}

			val directional = entity.directionalSprite()
			if (directional != null)
			{
				directional.directionalSprite.size = size
			}

			val additional = entity.additionalRenderable()
			if (additional != null)
			{
				for (r in additional.below.values())
				{
					r.size[0] = size
					r.size[1] = size
				}

				for (r in additional.above.values())
				{
					r.size[0] = size
					r.size[1] = size
				}
			}
		}
	}

	override fun reset()
	{
		offset.set(0f, 0f)
		turnsOnTile = 0
		moveLocked = false
		facing = Direction.SOUTH
		position = Point(-1, -1)
		slot = SpaceSlot.ENTITY
		size = 1
		max = Point(-1, -1)
		moveable = true
		canFall = true
	}
}