package com.lyeeedar.AI.BehaviourTree.Nodes

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractBehaviourTreeItem
import com.lyeeedar.AI.BehaviourTree.Actions.AbstractBehaviourAction
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.DataClassCollection
import com.lyeeedar.Util.DataGraphNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set

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
	}
	override fun resolve(nodes: ObjectMap<String, AbstractBehaviourNode>)
	{
		super.resolve(nodes)
	}
	//endregion
}