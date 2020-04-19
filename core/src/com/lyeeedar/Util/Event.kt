package com.lyeeedar.Util

import com.badlogic.gdx.utils.Array

/**
 * Created by Philip on 13-Jul-16.
 */

enum class HandlerAction
{
	KeepAttached,
	Detach
}

class Event0Arg {
	private val handlers = Array<(() -> HandlerAction)>(false, 4)

	operator fun plusAssign(handler: () -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot add handlers during invoke!")
		handlers.add(handler)
	}

	operator fun minusAssign(handler: () -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot remove handlers during invoke!")
		handlers.removeValue(handler, true)
	}

	var invoking = false
	operator fun invoke()
	{
		invoking = true

		val itr = handlers.iterator()
		while (itr.hasNext())
		{
			val handler = itr.next()
			if (handler.invoke() == HandlerAction.Detach) itr.remove()
		}

		invoking = false
	}

	fun clear()
	{
		handlers.clear()
	}
}

class Event1Arg<T> {
	private val handlers = Array<((T) -> HandlerAction)>(false, 4)

	operator fun plusAssign(handler: (T) -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot add handlers during invoke!")
		handlers.add(handler)
	}

	operator fun minusAssign(handler: (T) -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot remove handlers during invoke!")
		handlers.removeValue(handler, true)
	}

	var invoking = false
	operator fun invoke(value: T)
	{
		invoking = true

		val itr = handlers.iterator()
		while (itr.hasNext())
		{
			val handler = itr.next()
			if (handler.invoke(value) == HandlerAction.Detach) itr.remove()
		}

		invoking = false
	}

	fun clear()
	{
		handlers.clear()
	}
}

class Event2Arg<T1, T2> {
	private val handlers = Array<((T1, T2) -> HandlerAction)>(false, 4)

	operator fun plusAssign(handler: (T1, T2) -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot add handlers during invoke!")
		handlers.add(handler)
	}

	operator fun minusAssign(handler: (T1, T2) -> HandlerAction)
	{
		if (invoking) throw Exception("Cannot remove handlers during invoke!")
		handlers.removeValue(handler, true)
	}

	var invoking = false
	operator fun invoke(value1: T1, value2: T2)
	{
		invoking = true

		val itr = handlers.iterator()
		while (itr.hasNext())
		{
			val handler = itr.next()
			if (handler.invoke(value1, value2) == HandlerAction.Detach) itr.remove()
		}

		invoking = false
	}

	fun clear()
	{
		handlers.clear()
	}
}