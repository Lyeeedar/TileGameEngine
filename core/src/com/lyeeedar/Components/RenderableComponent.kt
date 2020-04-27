package com.lyeeedar.Components

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.round
import ktx.collections.set

inline fun Entity.sprite(): Sprite? = this.renderable()?.renderable as? Sprite
inline fun Entity.renderOffset() = this.renderable()?.renderable?.animation?.renderOffset(false)
class RenderableComponent(data: RenderableComponentData) : AbstractComponent<RenderableComponentData>(data)
{
	override val type: ComponentType = ComponentType.Renderable

	var dataRenderable: Renderable = data.renderable.copy()
	var renderable: Renderable
		get() = overrideRenderable ?: dataRenderable
		set(value)
		{
			overrideRenderable = value
		}
	private var overrideRenderable: Renderable? = null

	override fun onDataSwapped()
	{
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