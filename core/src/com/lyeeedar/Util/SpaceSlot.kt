package com.lyeeedar

// ----------------------------------------------------------------------
enum class SpaceSlot private constructor(val type: SpaceSlotType)
{
	FLOOR(SpaceSlotType.MAP),
	FLOORDETAIL(SpaceSlotType.MAP),
	WALL(SpaceSlotType.MAP),
	WALLDETAIL(SpaceSlotType.MAP),
	BELOWENTITY(SpaceSlotType.ENTITY),
	ENTITY(SpaceSlotType.ENTITY),
	ABOVEENTITY(SpaceSlotType.ENTITY),
	EFFECT(SpaceSlotType.OTHER),
	LIGHT(SpaceSlotType.OTHER);


	companion object
	{

		val Values = SpaceSlot.values()
		val MapValues = arrayOf(FLOOR, FLOORDETAIL, WALL, WALLDETAIL)
		val EntityValues = arrayOf(BELOWENTITY, ENTITY, ABOVEENTITY)
	}
}

enum class SpaceSlotType
{
	MAP,
	ENTITY,
	OTHER
}