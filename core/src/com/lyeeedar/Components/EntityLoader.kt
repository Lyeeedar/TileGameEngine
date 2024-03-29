package com.lyeeedar.Components

import com.lyeeedar.Util.getXml

class EntityLoader()
{
	companion object
	{
		@JvmStatic fun load(path: String): Entity
		{
			val xml = getXml(path)
			val data = EntityData()
			data.load(xml)

			return load(data)
		}

		fun load(data: EntityData): Entity
		{
			val entity = if (data.extends.isNotBlank()) load(data.extends) else EntityPool.obtain()

			entity.addComponent(ComponentType.LoadData)

			for (componentData in data.components)
			{
				val componentID = componentData.classID
				val componentType = ComponentType.valueOf(componentID)
				val component = entity.addComponent(componentType) as DataComponent
				component.initialiseFrom(componentData)
			}

			return entity
		}
	}
}