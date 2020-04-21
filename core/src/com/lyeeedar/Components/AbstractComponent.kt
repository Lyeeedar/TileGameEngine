package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
abstract class AbstractComponentData : XmlDataClass()
{
	abstract val classID: String

	//[generated]
	override fun load(xmlData: XmlData)
	{
	}

	companion object
	{
		fun loadPolymorphicClass(classID: String): AbstractComponentData
		{
			return when (classID)
			{
				"AdditionalRenderable" -> AdditionalRenderableComponentData()
				"DirectionalSprite" -> DirectionalSpriteComponentData()
				"Renderable" -> RenderableComponentData()
				"Name" -> NameComponentData()
				"Dialogue" -> DialogueComponentData()
				"Empty" -> EmptyComponentData()
				"Position" -> PositionComponentData()
				"MetaRegion" -> MetaRegionComponentData()
				else -> throw RuntimeException("Unknown classID '$classID' for AbstractComponentData!")
			}
		}
	}
	//[/generated]
}
class EmptyComponentData : AbstractComponentData()
{
	override val classID: String = "Empty"

	//[generated]
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	//[/generated]
}
abstract class AbstractComponent<T: AbstractComponentData>(var data: T)
{
	abstract val type: ComponentType

	fun swapData(data: AbstractComponentData)
	{
		this.data = data as T
		onDataSwapped()
	}
	open fun onDataSwapped() {}

	var obtained = false
	fun free()
	{
		ComponentPool.free(this)
	}

	abstract fun reset()
}