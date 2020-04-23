package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.BehaviourTree
import com.lyeeedar.AI.BehaviourTree.BehaviourTreeState
import com.lyeeedar.AI.BehaviourTree.EvaluationState
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.DataFileReference
import com.lyeeedar.Util.XmlData

class ImportBehaviourAction : AbstractBehaviourAction()
{
	@DataFileReference(resourceType = "BehaviourTree")
	lateinit var path: String

	//region non-data
	lateinit var importedTree: BehaviourTree
	//endregion

	fun afterLoad()
	{
		importedTree = BehaviourTree.load(path)
	}

	override fun evaluate(state: BehaviourTreeState): EvaluationState
	{
		return importedTree.root.evaluate(state)
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		path = xmlData.get("Path")
		afterLoad()
	}
	override val classID: String = "Import"
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}