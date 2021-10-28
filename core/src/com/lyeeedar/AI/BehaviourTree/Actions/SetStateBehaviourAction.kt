package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataGraphReference
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import java.util.*
import ktx.collections.set

@DataClass(category = "Flow Control")
class SetStateBehaviourAction : AbstractBehaviourAction()
{
	val outputMap: FastEnumMap<EvaluationState, EvaluationState> = FastEnumMap(EvaluationState::class.java)

	@DataGraphReference
	lateinit var node: AbstractBehaviourNode

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		val retState = node.evaluate(state)
		return outputMap[retState] ?: EvaluationState.COMPLETED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val outputMapEl = xmlData.getChildByName("OutputMap")
		if (outputMapEl != null)
		{
			for (el in outputMapEl.children)
			{
				val enumVal = EvaluationState.valueOf(el.name.uppercase(Locale.ENGLISH))
				val objoutputMap: EvaluationState
				objoutputMap = EvaluationState.valueOf(el.text.uppercase(Locale.ENGLISH))
				outputMap[enumVal] = objoutputMap
			}
		}
		nodeGUID = xmlData.get("Node")
	}
	override val classID: String = "SetState"
	private lateinit var nodeGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
		node = nodes[nodeGUID]!!
	}
	//endregion
}