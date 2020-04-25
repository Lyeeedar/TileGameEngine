package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.CompiledExpression
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.stats
import com.lyeeedar.Util.DataCompiledExpression
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import java.lang.RuntimeException

class PickOneFromBehaviourAction : AbstractBehaviourAction()
{
	lateinit var input: String
	lateinit var output: String

	@DataCompiledExpression(knownVariables = "dist,hp,level,damage", default = "dist")
	lateinit var condition: CompiledExpression

	var minimum: Boolean = true

	//region non-data
	val map = ObjectFloatMap<String>()
	//endregion

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		val array = state.getData<Array<*>>(input, 0) ?: return EvaluationState.FAILED
		if (array.size == 0) return EvaluationState.FAILED

		val comparator: ((Any) -> Float) = when (array[0])
		{
			is Entity -> fun (item: Any): Float {
				val entity = item as Entity

				map.clear()
				val dist = entity.pos()!!.position.taxiDist(state.entity.pos()!!.position)
				map.put("dist", dist.toFloat())
				entity.stats()?.write(map)

				return condition.evaluate(map)
			}
			is Point -> fun (item: Any): Float {
				val point = item as Point

				map.clear()
				val dist = point.taxiDist(state.entity.pos()!!.position)
				map.put("dist", dist.toFloat())

				return condition.evaluate(map)
			}
			else -> throw RuntimeException("Cannot pick one from array of " + array[0]::class.java.name)
		}

		val sorted = if (minimum) array.sortedBy(comparator) else array.sortedByDescending(comparator)
		val item = sorted.firstOrNull() ?: return EvaluationState.FAILED

		state.setData(output, 0, item)

		return EvaluationState.COMPLETED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		input = xmlData.get("Input")
		output = xmlData.get("Output")
		condition = CompiledExpression(xmlData.get("Condition"), "dist,hp,level,damage")
		minimum = xmlData.getBoolean("Minimum", true)
	}
	override val classID: String = "PickOneFrom"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}