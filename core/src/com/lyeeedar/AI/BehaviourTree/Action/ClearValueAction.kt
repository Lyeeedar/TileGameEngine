package com.lyeeedar.AI.BehaviourTree.Action

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.XmlData

class ClearValueAction : AbstractAction()
{
	lateinit var key: String

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		setData(key, null)

		state = ExecutionState.COMPLETED
		return state
	}

	override fun cancel(entity: Entity, world: World)
	{

	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		key = xmlData.get("Key")
	}
	override val classID: String = "ClearValue"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}