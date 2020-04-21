package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
abstract class AbstractComponentData : XmlDataClass()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
	}
	abstract val classID: String

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
	//endregion
}
class EmptyComponentData : AbstractComponentData()
{

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	override val classID: String = "Empty"
	//endregion
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