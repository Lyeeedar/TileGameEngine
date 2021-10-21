package com.lyeeedar.Components

import com.badlogic.gdx.utils.ObjectFloatMap
import com.lyeeedar.Util.set

class VariablesComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Variables

	val variables = ObjectFloatMap<String>()

	override fun reset()
	{
		variables.clear()
	}

	fun write(variableMap: ObjectFloatMap<String>, prefixName: String? = null)
	{
		val prefixName = if (prefixName != null) "$prefixName." else ""
		for (pair in variables)
		{
			variableMap[prefixName + pair.key] = pair.value
		}
	}
}