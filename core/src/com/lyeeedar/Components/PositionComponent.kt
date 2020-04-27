package com.lyeeedar.Components

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Direction
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import java.util.*

class PositionComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.Position

	var slot = SpaceSlot.ENTITY
	var moveable = true
	var size = 1

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

	var moveLocked = false

	var facing: Direction = Direction.SOUTH

	var turnsOnTile: Int = 0
	var lastPos = Point.MINUS_ONE

	val x: Int
		get() = position.x
	val y: Int
		get() = position.y

	override fun reset()
	{
		offset.set(0f, 0f)
		turnsOnTile = 0
		moveLocked = false
		facing = Direction.SOUTH
		position = Point(-1, -1)
		max = Point(-1, -1)
		lastPos = Point.MINUS_ONE
	}

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as PositionComponentData
		slot = data.slot
		moveable = data.moveable
		size = data.size
	}
}

class PositionComponentData : AbstractComponentData()
{
	var slot: SpaceSlot = SpaceSlot.ENTITY
	var moveable: Boolean = true
	var size: Int = 1

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		slot = SpaceSlot.valueOf(xmlData.get("Slot", SpaceSlot.ENTITY.toString())!!.toUpperCase(Locale.ENGLISH))
		moveable = xmlData.getBoolean("Moveable", true)
		size = xmlData.getInt("Size", 1)
	}
	override val classID: String = "Position"
	//endregion
}