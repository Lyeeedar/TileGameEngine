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
			val data = EntityData()
			data.load(xml)

			val entity = if (data.extends.isNotBlank()) load(data.extends, skipRenderables) else EntityPool.obtain()

			entity.addComponent(ComponentType.LoadData)
			entity.loadData()!!.set(path, xml, true)

			for (component in data.components)
			{
				val componentID = component.classID
				if (skipRenderables)
				{
					if (componentID.contains("Renderable") || componentID.contains("Sprite"))
					{
						continue
					}
				}

				val componentType = ComponentType.valueOf(componentID)
				entity.addComponent(componentType).swapData(component)
			}

			if (!entity.hasComponent(ComponentType.Name))
			{
				entity.addComponent(ComponentType.Name)
				entity.name()!!.data.name = path
			}

			return entity
		}
	}
}