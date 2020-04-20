package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
abstract class AbstractComponentData : XmlDataClass()
{

	override fun load(xmlData: XmlData)
	{
	/* Autogenerated method contents. Do not modify. */
	}

	companion object
	{
		fun loadPolymorphicClass(classID: String): AbstractComponentData
		{
		/* Autogenerated method contents. Do not modify. */
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
}
class EmptyComponentData : AbstractComponentData()
{
	val classID: String = "Empty"

	override fun load(xmlData: XmlData)
	{
	/* Autogenerated method contents. Do not modify. */
		super.load(xmlData)
	}
}
abstract class AbstractComponent<T: AbstractComponentData>(var data: T)
{
	abstract val type: ComponentType

	fun swapData(data: AbstractComponentData)
	{
		this.data = data as T
	}

	var obtained = false
	fun free()
	{
		ComponentPool.free(this)
	}

	abstract fun reset()
}