package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Util.EnumBitflag

class EntitySignature(private val all: EnumBitflag<ComponentType>, private val any: EnumBitflag<ComponentType>)
{
	val entities = Array<Entity>(false, 16)

	fun matches(entity: Entity): Boolean
	{
		if (all.bitFlag != 0 && !entity.signature.containsAll(all))
		{
			return false
		}
		if (any.bitFlag != 0 && !entity.signature.containsAny(any))
		{
			return false
		}

		return true
	}

	fun isEqual(other: EntitySignature): Boolean
	{
		return all.bitFlag == other.all.bitFlag && any.bitFlag == other.any.bitFlag
	}
}