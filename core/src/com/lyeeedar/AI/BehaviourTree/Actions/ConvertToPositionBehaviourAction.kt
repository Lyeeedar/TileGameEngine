package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.pos
import com.lyeeedar.Util.XmlData
import java.lang.RuntimeException

class ConvertToPositionBehaviourAction : AbstractBehaviourAction()
{
	lateinit var input: String
	lateinit var output: String

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		val oldValue = state.getData<Any>(input, 0) ?: return EvaluationState.FAILED

		if (oldValue is Entity)
		{
			val pos = oldValue.pos()!!.position
			state.setData(output, 0, pos)
		}
		else
		{
			throw RuntimeException("Unable to convert " + oldValue::class.java.name + " to a position")
		}

		return EvaluationState.COMPLETED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		input = xmlData.get("Input")
		output = xmlData.get("Output")
	}
	override val classID: String = "ConvertToPosition"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}