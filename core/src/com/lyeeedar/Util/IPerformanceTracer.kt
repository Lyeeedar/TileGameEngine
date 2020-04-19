package com.lyeeedar.Util

interface ITrace
{
	fun start()
	fun stop()
}

interface IPerformanceTracer
{
	fun getTrace(name: String): ITrace
}

class DummyTrace : ITrace
{
	override fun start()
	{

	}

	override fun stop()
	{

	}
}

class DummyPerformanceTracer : IPerformanceTracer
{
	override fun getTrace(name: String): ITrace
	{
		return DummyTrace()
	}

}