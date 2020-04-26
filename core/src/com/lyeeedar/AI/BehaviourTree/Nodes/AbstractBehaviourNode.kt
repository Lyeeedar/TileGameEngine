package com.lyeeedar.AI.BehaviourTree.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractBehaviourTreeItem
import com.lyeeedar.AI.BehaviourTree.Actions.AbstractBehaviourAction
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

@DataGraphNode
@DataClassCollection
abstract class AbstractBehaviourNode : AbstractBehaviourTreeItem()
{
	//region non-data
	abstract val actions: Array<AbstractBehaviourAction>
	//endregion

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		afterLoad()
	}
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}