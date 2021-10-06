package com.lyeeedar.Components

import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData

inline fun Entity.sprite(): Sprite? = this.renderable()?.renderable as? Sprite
inline fun Entity.renderOffset() = this.renderable()?.renderable?.animation?.renderOffset(false)
class RenderableComponent : DataComponent()
{
	override val type: ComponentType = ComponentType.Renderable

	var dataRenderable: Renderable = defaultRenderable
	var renderable: Renderable
		get() = overrideRenderable ?: dataRenderable
		set(value)
		{
			overrideRenderable = value
		}
	private var overrideRenderable: Renderable? = null

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as RenderableComponentData
		dataRenderable = data.renderable.copy()
	}

	fun set(renderable: Renderable): RenderableComponent
	{
		dataRenderable = renderable
		return this
	}

	override fun reset()
	{
		overrideRenderable = null
	}

	companion object
	{
		private val defaultRenderable = AssetManager.loadSprite("blank")
	}
}

@DataClass(name = "RenderableComponent")
class RenderableComponentData : AbstractComponentData()
{
	var renderable: Renderable = AssetManager.loadSprite("blank")
		private set(value)
		{
			field = value
		}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		renderable = AssetManager.tryLoadRenderable(xmlData.getChildByName("Renderable"))!!
	}
	override val classID: String = "Renderable"
	//endregion
}