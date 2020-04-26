package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*

abstract class AbstractRenderSystem(world: World<*>) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.Renderable).get())
{
	val tileSize: Float
		get() = world.tileSize

	protected val ambientLight = Colour.WHITE
	protected val batch: SpriteBatch by lazy { SpriteBatch() }
	val renderer: SortedRenderer by lazy { SortedRenderer(tileSize, Statics.resolution[0] / tileSize, Statics.resolution[1] / tileSize, SpaceSlot.Values.size, false) }

	protected var playerOffsetX: Float = 0f
	protected var playerOffsetY: Float = 0f
	protected var offsetx: Float = 0f
	protected var offsety: Float = 0f

	private var renderedStaticOffsetX: Float = -10000f
	private var renderedStaticOffsetY: Float = -10000f

	override fun beforeUpdate(deltaTime: Float)
	{
		val playerOffset = getPlayerPosition()
		playerOffsetX = playerOffset.x
		playerOffsetY = playerOffset.y

		offsetx = (Statics.resolution.x * 0.5f) - (playerOffsetX * tileSize) - (tileSize * 0.5f)
		offsety = (Statics.resolution.y * 0.5f) - (playerOffsetY * tileSize) - (tileSize * 0.5f)

		val screenTileWidth = (Statics.resolution.x / tileSize).toInt() + 2
		val screenTileHeight = (Statics.resolution.y / tileSize).toInt() + 2
		val xs = max(0, playerOffsetX.toInt()-screenTileWidth/2)
		val xe = min(world.grid.width, playerOffsetX.toInt()+screenTileWidth/2)
		val ys = max(0, playerOffsetY.toInt()-screenTileHeight/2)
		val ye = min(world.grid.height, playerOffsetY.toInt()+screenTileHeight/2)

		if (renderedStaticOffsetX != offsetx || renderedStaticOffsetY != offsety)
		{
			renderedStaticOffsetX = offsetx
			renderedStaticOffsetY = offsety

			renderer.beginStatic(offsetx, offsety, ambientLight)

			for (x in xs until xe)
			{
				for (y in ys until ye)
				{
					val floor = world.grid[x, y].floor ?: continue
					renderer.queueSpriteWrapper(floor, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal)
				}
			}

			renderer.endStatic()
		}

		renderer.begin(deltaTime, offsetx, offsety, ambientLight)

		for (x in xs until xe)
		{
			for (y in ys until ye)
			{
				val wall = world.grid[x, y].wall
				if (wall != null)
				{
					renderer.queueSpriteWrapper(wall, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal)
				}
			}
		}
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val renderable = entity.renderable()!!.renderable
		val pos = entity.position()!!

		val px = pos.position.x.toFloat()
		val py = pos.position.y.toFloat()

		if (pos.position.taxiDist(playerOffsetX.toInt(), playerOffsetY.toInt()) > 50)
		{
			renderable.animation = null
			if (entity.components.containsKey(ComponentType.Transient))
			{
				entity.markForDeletion(0f, "Out of Sight")
			}

			return
		}

		if (renderable is ParticleEffect)
		{
			if (renderable.completed && entity.components.containsKey(ComponentType.Transient) && renderable.complete())
			{
				entity.markForDeletion(0f, "completed")
			}
		}
		renderable.size[0] = pos.data.size
		renderable.size[1] = pos.data.size

		renderer.queue(renderable, px, py, pos.data.slot.ordinal, 1)

		val offset = renderable.animation?.renderOffset(false)

		var ax = px
		var ay = py

		if (offset != null)
		{
			ax += offset[0]
			ay += offset[1]
		}

		val additional = entity.additionalRenderable()
		if (additional != null)
		{
			for (below in additional.below.values())
			{
				renderer.queue(below, ax, ay, pos.data.slot.ordinal, 0)
			}

			for (above in additional.above.values())
			{
				renderer.queue(above, ax, ay, pos.data.slot.ordinal, 2)
			}
		}

		drawExtraEntity(entity, deltaTime)
	}

	override fun afterUpdate(deltaTime: Float)
	{
		renderer.end(batch)
	}

	abstract fun drawExtraEntity(entity: Entity, deltaTime: Float)

	abstract fun getPlayerPosition(): Vector2

	override fun onTurnEntity(entity: Entity)
	{

	}

	fun getClickTile(screenX: Int, screenY: Int): AbstractTile?
	{
		val playerPos = getPlayerPosition()

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 0.5f

		val mousex = ((screenX - offsetx) / tileSize).toInt()
		val mousey = (((Statics.resolution[1] - screenY) - offsety) / tileSize).toInt()

		return world.grid.tryGet(mousex, mousey, null)
	}
}