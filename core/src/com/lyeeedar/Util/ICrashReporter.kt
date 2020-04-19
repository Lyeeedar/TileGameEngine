package com.lyeeedar.Util

interface ICrashReporter
{
	fun setCustomKey(key: String, value: String)
	fun setCustomKey(key: String, value: Boolean)
	fun setCustomKey(key: String, value: Double)
	fun setCustomKey(key: String, value: Float)
	fun setCustomKey(key: String, value: Int)
}



class DummyCrashReporter : ICrashReporter
{
	override fun setCustomKey(key: String, value: String)
	{

	}

	override fun setCustomKey(key: String, value: Boolean)
	{

	}

	override fun setCustomKey(key: String, value: Double)
	{

	}

	override fun setCustomKey(key: String, value: Float)
	{

	}

	override fun setCustomKey(key: String, value: Int)
	{

	}
}