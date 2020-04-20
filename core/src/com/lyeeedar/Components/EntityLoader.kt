package com.lyeeedar.Components

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.directory
import com.lyeeedar.Util.getXml
import java.util.*

class EntityLoader()
{
	companion object
	{
		@JvmStatic fun load(path: String, skipRenderables: Boolean): Entity
		{
			val xml = getXml(path)

			val entity = if (xml.get("Extends", null) != null) load(xml.get("Extends"), skipRenderables) else EntityPool.obtain()

			entity.addComponent(ComponentType.LoadData)
			entity.loadData()!!.set(path, xml, true)

			val componentsEl = xml.getChildByName("Components") ?: return entity

			for (componentEl in componentsEl.children())
			{
				if (skipRenderables)
				{
					if (
						componentEl.name.toUpperCase(Locale.ENGLISH) == "ADDITIONALRENDERABLES" ||
						componentEl.name.toUpperCase(Locale.ENGLISH) == "DIRECTIONALSPRITE" ||
						componentEl.name.toUpperCase(Locale.ENGLISH) == "RENDERABLE")
					{
						continue
					}
				}

				val componentID = componentEl.get("classID")
				val componentType = ComponentType.valueOf(componentID)
				val component = entity.addComponent(componentType)
				val data = AbstractComponentData.loadPolymorphicClass(componentID)
				data.load(componentEl)
				component.swapData(data)
			}

			if (!entity.hasComponent(ComponentType.Name))
			{
				entity.addComponent(ComponentType.Name)
				entity.name().data.name = path
			}

			return entity
		}
	}
}