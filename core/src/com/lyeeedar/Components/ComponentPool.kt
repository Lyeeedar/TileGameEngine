package com.lyeeedar.Components

import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Util.FastEnumMap

class ComponentPool
{
	companion object
	{
		val pools = FastEnumMap<ComponentType, Pool<AbstractComponent>>(ComponentType::class.java)

		fun obtain(type: ComponentType): AbstractComponent
		{
			val pool = pools[type]

			val obj = pool.obtain()
			if (obj.obtained) throw RuntimeException()
			obj.obtained = true

			obj.reset()

			return obj
		}

		fun free(component: AbstractComponent)
		{
			if (!component.obtained) throw RuntimeException()
			component.obtained = false
			pools[component.type].free(component)
		}

		init
		{
			for (type in ComponentType.Values)
			{
				pools[type] = object : Pool<AbstractComponent>()
				{
					override fun newObject(): AbstractComponent
					{
						return type.constructor.invoke()
					}
				}
			}
		}
	}
}