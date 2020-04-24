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

class EntitySignatureBuilder(val world: World)
{
	private val all = EnumBitflag<ComponentType>()
	private val any = EnumBitflag<ComponentType>()

	fun all(vararg types: ComponentType): EntitySignatureBuilder
	{
		for (type in types)
		{
			this.all.setBit(type)
		}

		return this
	}

	fun any(vararg types: ComponentType): EntitySignatureBuilder
	{
		for (type in types)
		{
			this.any.setBit(type)
		}

		return this
	}

	fun get(): EntitySignature
	{
		val signature = EntitySignature(all, any)

		for (existing in world.registeredSignatures)
		{
			if (existing.isEqual(signature))
			{
				return existing
			}
		}

		world.registeredSignatures.add(signature)
		return signature
	}
}