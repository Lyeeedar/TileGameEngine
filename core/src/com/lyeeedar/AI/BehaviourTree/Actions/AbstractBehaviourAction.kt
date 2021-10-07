package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractBehaviourTreeItem
import com.lyeeedar.AI.BehaviourTree.Nodes.AbstractBehaviourNode
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

abstract class AbstractBehaviourAction : AbstractBehaviourTreeItem()
{

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