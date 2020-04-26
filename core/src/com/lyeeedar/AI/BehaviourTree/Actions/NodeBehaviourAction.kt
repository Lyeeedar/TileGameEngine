package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

class NodeBehaviourAction : AbstractBehaviourAction()
{
	@DataGraphReference
	lateinit var node: AbstractBehaviourNode

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		return node.evaluate(state)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		nodeGUID = xmlData.get("Node")
		afterLoad()
	}
	override val classID: String = "Node"
	lateinit var nodeGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
		node = nodes[nodeGUID]!!
	}
	//endregion
}