package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.GraphXmlDataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

abstract class AbstractBehaviourTreeItem : GraphXmlDataClass<AbstractBehaviourNode>()
{
	//region non-data
	var dataGuid: Int = 0
	//endregion

	protected fun wasEvaluatedLastTime(state: BehaviourTreeState): Boolean
	{
		val lastID = state.getData("id", dataGuid, -1)!!
		val runLastTime = lastID != state.lastEvaluationID
		state.setData("id", dataGuid, state.evaluationID)

		return runLastTime
	}

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