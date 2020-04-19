package com.lyeeedar.Components

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.collections.set

inline fun Entity.additionalRenderable(): AdditionalRenderableComponent? = this.components[ComponentType.AdditionalRenderable] as AdditionalRenderableComponent?
class AdditionalRenderableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.AdditionalRenderable

	val below = ObjectMap<String, Renderable>()
	val above = ObjectMap<String, Renderable>()

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		val pos = entity.pos()

		val belowEls = xml.getChildByName("Below")
		if (belowEls != null)
		{
			for (el in belowEls.children())
			{
				val key = el.get("Key")
				val renderable = AssetManager.loadRenderable(el.getChildByName("Renderable")!!)

				if (pos != null)
				{
					renderable.size[0] = pos.size
					renderable.size[1] = pos.size
				}

				below[key] = renderable
			}
		}

		val aboveEls = xml.getChildByName("Above")
		if (aboveEls != null)
		{
			for (el in aboveEls.children())
			{
				val key = el.get("Key")
				val renderable = AssetManager.loadRenderable(el.getChildByName("Renderable")!!)

				if (pos != null)
				{
					renderable.size[0] = pos.size
					renderable.size[1] = pos.size
				}

				above[key] = renderable
			}
		}
	}

	override fun reset()
	{
		below.clear()
		above.clear()
	}
}
