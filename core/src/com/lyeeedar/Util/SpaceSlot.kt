package com.lyeeedar

// ----------------------------------------------------------------------
enum class SpaceSlot
{
	FLOOR,
	FLOORDETAIL,
	WALL,
	WALLDETAIL,
	BELOWENTITY,
	ENTITY,
	ABOVEENTITY,
	EFFECT,
	LIGHT;


	companion object
	{

		val Values = SpaceSlot.values()
		val BasicValues = arrayOf(FLOOR, FLOORDETAIL, WALL, WALLDETAIL)
		val EntityValues = arrayOf(BELOWENTITY, ENTITY, ABOVEENTITY)
	}
}