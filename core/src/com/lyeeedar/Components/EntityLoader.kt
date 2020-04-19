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
		val sharedRenderableMap = ObjectMap<Int, Renderable>()

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

				val componentType = ComponentType.Values.first { it.toString().equals(componentEl.name, ignoreCase = true) }
				val component: AbstractComponent = entity.addComponent(componentType)

				component.parse(componentEl, entity, path.directory())
			}

			if (!entity.hasComponent(ComponentType.Name))
			{
				entity.addComponent(ComponentType.Name)
				entity.name().set(path)
			}

			return entity
		}
	}
}