package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.Entity
import com.lyeeedar.Components.EntityPool
import com.lyeeedar.Game.Tile
import com.lyeeedar.Util.Array2D

class World
{
	var tileSize: Float = 40f

	val entities = Array<Entity>(false, 128)
	val toBeAdded = Array<Entity>(false, 128)
	val toBeRemoved = ObjectSet<Entity>()

	var player: Entity? = null

	val systems = Array<AbstractSystem>()

	var grid: Array2D<Tile> = Array2D(0, 0) { x, y -> Tile(x, y) }

	fun addEntity(entity: Entity)
	{
		toBeAdded.add(entity)
	}

	fun removeEntity(entity: Entity)
	{
		toBeRemoved.add(entity)
	}

	fun updateEntityList()
	{
		var i = 0
		while (i < entities.size)
		{
			if (toBeRemoved.contains(entities[i]))
			{
				entities.removeIndex(i)
				i--
			}

			i++
		}

		for (i in 0 until toBeAdded.size)
		{
			entities.add(toBeAdded[i])
		}

		toBeAdded.clear()
		toBeRemoved.clear()
	}

	fun update(delta: Float)
	{
		for (i in 0 until systems.size)
		{
			systems[i].update(delta)
		}

		updateEntityList()
		EntityPool.flushFreedEntities()
	}

	fun onTurn()
	{
		for (i in 0 until systems.size)
		{
			systems[i].onTurn()
		}
	}
}