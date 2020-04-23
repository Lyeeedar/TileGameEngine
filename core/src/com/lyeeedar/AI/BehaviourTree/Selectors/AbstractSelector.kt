package com.lyeeedar.AI.BehaviourTree.Selectors

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.AI.BehaviourTree.AbstractNodeContainer
import com.lyeeedar.AI.BehaviourTree.AbstractTreeNode
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader

@DataClass(category = "Selectors")
@DataGraphNode
@DataClassCollection
abstract class AbstractSelector : AbstractNodeContainer()
{
	val children: Array<AbstractTreeNode> = Array(1)

	// ----------------------------------------------------------------------
	override fun <T> findData(key: String): T?
	{
		val thisVar = super.findData<T>(key)
		if (thisVar != null)
		{
			return thisVar
		}

		for (node in children)
		{
			val nodeVar = node.findData<T>(key)
			if (nodeVar != null)
			{
				return nodeVar
			}
		}

		return null
	}

	// ----------------------------------------------------------------------
	fun addNode( node: AbstractTreeNode)
	{
		if ( node.data == null )
		{
			node.data = data
		}
		node.parent = this
		children.add( node )
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val childrenEl = xmlData
		if (childrenEl != null)
		{
			for (el in childrenEl.children)
			{
				val objchildren: AbstractTreeNode
				val objchildrenEl = xmlData.getChildByName("Children")!!
				objchildren = XmlDataClassLoader.loadAbstractTreeNode(objchildrenEl.get("classID"))
				objchildren.load(objchildrenEl)
				children.add(objchildren)
			}
		}
	}
	override fun resolve(nodes: ObjectMap<String, AbstractNodeContainer>)
	{
		super.resolve(nodes)
		for (item in children)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}