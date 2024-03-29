package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set

@DataClass(category = "Flow Control")
class BranchBehaviourAction : AbstractBehaviourAction()
{
	val branches: Array<ConditionAndNode> = Array()

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		for (i in 0 until branches.size)
		{
			val branch = branches[i]
			if (branch.condition.evaluate(state.getVariables(), state.rng).round() != 0)
			{
				return branch.node.evaluate(state)
			}
		}

		return EvaluationState.COMPLETED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val branchesEl = xmlData.getChildByName("Branches")
		if (branchesEl != null)
		{
			for (el in branchesEl.children)
			{
				val objbranches: ConditionAndNode
				val objbranchesEl = el
				objbranches = ConditionAndNode()
				objbranches.load(objbranchesEl)
				branches.add(objbranches)
			}
		}
	}
	override val classID: String = "Branch"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
		for (item in branches)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}

class ConditionAndNode : GraphXmlDataClass<AbstractBehaviourNode>()
{
	@DataCompiledExpression(knownVariables = "else")
	lateinit var condition: CompiledExpression

	@DataGraphReference(useParentDescription = true)
	lateinit var node: AbstractBehaviourNode

	//region generated
	override fun load(xmlData: XmlData)
	{
		condition = CompiledExpression(xmlData.get("Condition", "1")!!)
		nodeGUID = xmlData.get("Node")
	}
	private lateinit var nodeGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		node = nodes[nodeGUID]!!
	}
	//endregion
}