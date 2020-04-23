package com.lyeeedar.AI.BehaviourTree.Decorators

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.BehaviourTree
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Entity
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.DataFileReference
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlData.Companion.getXml

class ImportDecorator : AbstractDecorator()
{
	@DataFileReference(resourceType = "BehaviourTree")
	lateinit var path: String

	override fun evaluate(entity: Entity, world: World): ExecutionState
	{
		if (node == null)
		{
			val tree = BehaviourTree()
			tree.load(getXml(path))

			node = tree.root
		}

		return node?.evaluate(entity, world) ?: ExecutionState.FAILED
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		path = xmlData.get("Path")
	}
	override val classID: String = "Import"
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}