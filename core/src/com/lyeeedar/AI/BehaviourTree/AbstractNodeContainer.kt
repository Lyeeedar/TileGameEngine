package com.lyeeedar.AI.BehaviourTree

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
abstract class AbstractNodeContainer : AbstractTreeNode()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
	}
	//endregion
}