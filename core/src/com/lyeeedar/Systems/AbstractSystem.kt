package com.lyeeedar.Systems

import com.lyeeedar.UI.DebugConsole

abstract class AbstractSystem(val world: World<*>)
{
	var processDuration: Float = 0f
	fun update(deltaTime: Float)
	{
		val start = System.nanoTime()

		doUpdate(deltaTime)

		val end = System.nanoTime()
		val diff = (end - start) / 1000000000f

		processDuration = (processDuration + diff) / 2f
	}
	abstract fun doUpdate(deltaTime: Float)

	open fun onTurn()
	{

	}

	open fun onLevelChanged()
	{

	}

	open fun registerDebugCommands(debugConsole: DebugConsole)
	{

	}

	open fun unregisterDebugCommands(debugConsole: DebugConsole)
	{

	}
}