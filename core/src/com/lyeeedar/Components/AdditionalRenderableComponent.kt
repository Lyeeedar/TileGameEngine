package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.collections.set

inline fun Entity.additionalRenderable(): AdditionalRenderableComponent? = this.components[ComponentType.AdditionalRenderable] as AdditionalRenderableComponent?
class AdditionalRenderableComponent(data: AdditionalRenderableComponentData) : AbstractComponent<AdditionalRenderableComponentData>(data)
{
	override val type: ComponentType = ComponentType.AdditionalRenderable

	val below = ObjectMap<String, Renderable>()
	val above = ObjectMap<String, Renderable>()

	fun updateMaps()
	{
		below.clear()
		above.clear()

		for (r in data.below)
		{
			below[r.hashCode().toString()] = r
		}

		for (r in data.above)
		{
			above[r.hashCode().toString()] = r
		}
	}

	override fun reset()
	{
		updateMaps()
	}

	override fun onDataSwapped()
	{
		updateMaps()
	}
}

class AdditionalRenderableComponentData : AbstractComponentData()
{
	val below: Array<Renderable> = Array<Renderable>()
	val above: Array<Renderable> = Array<Renderable>()

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val belowEl = xmlData.getChildByName("Below")
		if (belowEl != null)
		{
			for (el in belowEl.children)
			{
				val obj = AssetManager.loadRenderable(el)
				below.add(obj)
			}
		}
		val aboveEl = xmlData.getChildByName("Above")
		if (aboveEl != null)
		{
			for (el in aboveEl.children)
			{
				val obj = AssetManager.loadRenderable(el)
				above.add(obj)
			}
		}
	}
	override val classID: String = "AdditionalRenderable"
	//endregion
}