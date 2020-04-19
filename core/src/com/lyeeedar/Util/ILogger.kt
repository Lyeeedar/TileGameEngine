package com.lyeeedar.Util

interface ILogger
{
	fun logDebug(message: String)
	fun logWarning(message: String)
	fun logError(message: String)
}

class DummyLogger : ILogger
{
	override fun logDebug(message: String)
	{

	}

	override fun logWarning(message: String)
	{

	}

	override fun logError(message: String)
	{

	}
}