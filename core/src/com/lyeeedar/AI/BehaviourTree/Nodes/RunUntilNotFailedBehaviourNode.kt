package com.lyeeedar.AI.BehaviourTree.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Actions.AbstractBehaviourAction
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

class RunUntilNotFailedBehaviourNode : AbstractBehaviourNode()
{
	override val actions: Array<AbstractBehaviourAction> = Array()

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		var retState = EvaluationState.FAILED
		for (i in 0 until actions.size)
		{
			val childState = actions[i].evaluate(state)
			if (childState != EvaluationState.FAILED)
			{
				retState = childState
				break
			}
		}

		return retState
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val actionsEl = xmlData
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val objactions: AbstractBehaviourAction
				val objactionsEl = el
				objactions = XmlDataClassLoader.loadAbstractBehaviourAction(objactionsEl.get("classID", objactionsEl.name)!!)
				objactions.load(objactionsEl)
				actions.add(objactions)
			}
		}
	}
	override val classID: String = "RunUntilNotFailed"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
		for (item in actions)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}