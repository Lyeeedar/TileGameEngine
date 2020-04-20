package com.lyeeedar.Systems

import com.lyeeedar.Components.ComponentType
import com.lyeeedar.Components.Entity
import com.lyeeedar.Util.EnumBitflag

class EntitySignature
{
	private val all = EnumBitflag<ComponentType>()
	private val any = EnumBitflag<ComponentType>()

	fun all(vararg types: ComponentType): EntitySignature
	{
		for (type in types)
		{
			this.all.setBit(type)
		}

		return this
	}

	fun any(vararg types: ComponentType): EntitySignature
	{
		for (type in types)
		{
			this.any.setBit(type)
		}

		return this
	}

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
}