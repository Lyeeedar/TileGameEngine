package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

abstract class AbstractBehaviourAction : GraphXmlDataClass<AbstractBehaviourNode>()
{
	//region non-data
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