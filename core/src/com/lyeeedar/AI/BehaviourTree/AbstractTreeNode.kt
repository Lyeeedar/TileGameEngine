package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import squidpony.squidmath.LightRNG

abstract class AbstractTreeNode : GraphXmlDataClass<AbstractNodeContainer>()
{
	//region non-data
	lateinit var ran: LightRNG
	var parent: AbstractNodeContainer? = null
	var state: ExecutionState = ExecutionState.NONE
	var data: ObjectMap<String, Any>? = null
	//endregion

	//----------------------------------------------------------------------
	fun setData(key:String, value:Any?)
	{
		val oldVal = data?.get(key)
		if (oldVal != value && oldVal is Point)
		{
			oldVal.free()
		}

		data?.put(key, value)
	}

	//----------------------------------------------------------------------
	private val variables = ObjectFloatMap<String>()
	fun getVariableMap(entity: Entity): ObjectFloatMap<String>
	{
		variables.clear()

		val parentmap = parent?.getVariableMap(entity)
		if (parentmap != null)
		{
			for (pair in parentmap)
			{
				variables.put(pair.key, pair.value)
			}
		}

		for (entry in data!!)
		{
			if (entry.value is Float)
			{
				variables.put(entry.key, entry.value as Float)
			}
			else if (entry.value is Int)
			{
				variables.put(entry.key, (entry.value as Int).toFloat())
			}
			else if (entry.value is Boolean)
			{
				variables.put(entry.key, if(entry.value as Boolean) 1f else 0f)
			}
			else
			{
				variables.put(entry.key, 1f)
			}
		}

		return variables
	}

	//----------------------------------------------------------------------
	fun <T> getData(key:String, fallback:T? = null): T?
	{
		val parentVar = parent?.getData<T>(key, null)

		val thisVar = data?.get(key) as? T

		return thisVar ?: parentVar ?: fallback
	}

	//----------------------------------------------------------------------
	open fun <T> findData(key: String): T?
	{
		val thisVar = data?.get(key) as? T
		if (thisVar != null)
		{
			return thisVar
		}

		return null
	}

	abstract fun evaluate(entity: Entity, world: World): ExecutionState
	abstract fun cancel(entity: Entity, world: World)

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
	}
	//endregion
}

enum class ExecutionState
{
	NONE,
	RUNNING,
	COMPLETED,
	FAILED
}