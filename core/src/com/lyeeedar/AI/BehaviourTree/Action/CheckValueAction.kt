package com.lyeeedar.AI.BehaviourTree.Action

import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData
import java.util.*

class CheckValueAction : AbstractAction()
{
	lateinit var condition: CompiledExpression
	var succeed: ExecutionState = ExecutionState.COMPLETED
	var fail: ExecutionState = ExecutionState.FAILED

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		val variableMap = getVariableMap(entity)

		val conditionVal = condition.evaluate(variableMap)

		state = if (conditionVal != 0f) succeed else fail
		return state
	}

	override fun cancel(entity: Entity, world: World)
	{

	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		condition = CompiledExpression(xmlData.get("Condition"))
		succeed = ExecutionState.valueOf(xmlData.get("Succeed", ExecutionState.COMPLETED.toString())!!.toUpperCase(Locale.ENGLISH))
		fail = ExecutionState.valueOf(xmlData.get("Fail", ExecutionState.FAILED.toString())!!.toUpperCase(Locale.ENGLISH))
	}
	override val classID: String = "CheckValue"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}