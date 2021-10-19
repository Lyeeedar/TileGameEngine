package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.CompiledExpression
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Data")
class SetValueBehaviourAction : AbstractBehaviourAction()
{
	lateinit var key: String
	lateinit var value: CompiledExpression

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		state.setData(key, 0, value.evaluate(state.getVariables(), state.rng.nextLong()))
		return EvaluationState.COMPLETED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		key = xmlData.get("Key")
		value = CompiledExpression(xmlData.get("Value", "1")!!)
	}
	override val classID: String = "SetValue"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}