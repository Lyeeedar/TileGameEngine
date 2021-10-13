package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import ktx.collections.set

class AdditionalRenderableComponent() : DataComponent()
{
	override val type: ComponentType = ComponentType.AdditionalRenderable

	val below = ObjectMap<String, Renderable>()
	val above = ObjectMap<String, Renderable>()

	override fun initialiseFrom(data: AbstractComponentData)
	{
		val data = data as AdditionalRenderableComponentData

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
		below.clear()
		above.clear()
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
				val objbelow: Renderable
				objbelow = AssetManager.tryLoadRenderable(el)!!
				below.add(objbelow)
			}
		}
		val aboveEl = xmlData.getChildByName("Above")
		if (aboveEl != null)
		{
			for (el in aboveEl.children)
			{
				val objabove: Renderable
				objabove = AssetManager.tryLoadRenderable(el)!!
				above.add(objabove)
			}
		}
	}
	override val classID: String = "AdditionalRenderable"
	//endregion
}