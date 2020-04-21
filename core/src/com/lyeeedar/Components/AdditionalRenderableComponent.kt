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
	override val classID: String = "AdditionalRenderable"

	val below = Array<Renderable>()
	val above = Array<Renderable>()

	//[generated]
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
	}
	//[/generated]
}