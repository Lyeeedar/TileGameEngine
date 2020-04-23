package com.lyeeedar.AI.BehaviourTree.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Actions.AbstractBehaviourAction
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

@DataGraphNode
@DataClassCollection
abstract class AbstractBehaviourNode : GraphXmlDataClass<AbstractBehaviourNode>()
{
	//region non-data
	abstract val actions: Array<AbstractBehaviourAction>
	var dataGuid: Int = 0
	//endregion

	abstract fun evaluate(state: BehaviourTreeState): EvaluationState

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
	}
	//endregion
}