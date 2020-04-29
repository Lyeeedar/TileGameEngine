package com.lyeeedar.AI.BehaviourTree.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Actions.AbstractBehaviourAction
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import com.lyeeedar.Util.removeRandom

@DataClass(category = "Other")
class RunOneRandomlyBehaviourNode : AbstractBehaviourNode()
{
	override val actions: Array<AbstractBehaviourAction> = Array()

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		var retState = EvaluationState.FAILED

		// read data
		var i = state.getData("i", dataGuid, -1)!!
		var numList = state.getData<Array<Int>>("numList", dataGuid, null)
		if (numList == null)
		{
			numList = Array()
			state.setData("numList", dataGuid, numList)
		}

		// cancel state if not entered last evaluation
		if (!wasEvaluatedLastTime(state))
		{
			i = -1
		}

		// do update
		if (i == -1)
		{
			numList.clear()
			for (n in 0 until actions.size) { numList.add(n); }

			while (retState == EvaluationState.FAILED && numList.size > 0)
			{
				i = numList.removeRandom(state.rng)
				retState = actions.get(i).evaluate(state)
			}
		}
		else
		{
			retState = actions.get(i).evaluate(state)
		}

		// store data
		if (retState != EvaluationState.RUNNING)
		{
			state.removeData("i", dataGuid)
		}
		else
		{
			state.setData("i", dataGuid, i)
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
	override val classID: String = "RunOneRandomly"
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