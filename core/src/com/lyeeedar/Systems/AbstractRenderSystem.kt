package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.ShadowCastCache
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.*
import java.awt.Color

abstract class AbstractRenderSystem(world: World<*>) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.Renderable).get())
{
	val tileSize: Float
		get() = world.tileSize

	protected val ambientLight = Colour.WHITE
	val renderer: SortedRenderer by lazy { SortedRenderer(tileSize, world.grid.width.toFloat(), world.grid.height.toFloat(), SpaceSlot.Values.size, true) }

	protected var playerOffsetX: Float = 0f
	protected var playerOffsetY: Float = 0f
	protected var offsetx: Float = 0f
	protected var offsety: Float = 0f

	private var renderedStaticOffsetX: Float = -10000f
	private var renderedStaticOffsetY: Float = -10000f

	val visionShadowCast = ShadowCastCache()
	val visionSet = IntSet()
	val seenSet = IntSet()
	val tempCol = Colour()
	override fun beforeUpdate(deltaTime: Float)
	{
		val playerOffset = getPlayerPosition()
		playerOffsetX = playerOffset.x
		playerOffsetY = playerOffset.y

		val screenTileWidth = (Statics.resolution.x.toFloat() / tileSize).toInt() + 4
		val screenTileHeight = (Statics.resolution.y.toFloat() / tileSize).toInt() + 4

		val rawCast = visionShadowCast.getShadowCast(playerOffset.x.toInt(), playerOffset.y.toInt(), max(screenTileWidth, screenTileHeight)/2)
		visionSet.clear()
		for (point in rawCast)
		{
			val hash = point.hashCode()
			visionSet.add(hash)
			seenSet.add(hash)
		}

		offsetx = (Statics.resolution.x * 0.5f) - (playerOffsetX * tileSize) - (tileSize * 0.5f)
		offsety = (Statics.resolution.y * 0.5f) - (playerOffsetY * tileSize) - (tileSize * 0.5f)

		val xs = playerOffsetX.toInt()-screenTileWidth/2
		val xe = playerOffsetX.toInt()+screenTileWidth/2
		val ys = playerOffsetY.toInt()-screenTileHeight/2
		val ye = playerOffsetY.toInt()+screenTileHeight/2

		if (renderedStaticOffsetX != offsetx || renderedStaticOffsetY != offsety)
		{
			renderedStaticOffsetX = offsetx
			renderedStaticOffsetY = offsety

			renderer.beginStatic(offsetx, offsety, ambientLight)

			for (x in xs until xe)
			{
				for (y in ys until ye)
				{
					val tile = world.grid.tryGet(x, y, null) ?: continue
					val floor = tile.floor ?: continue

					val tileHash = tile.hashCode()
					if (!seenSet.contains(tileHash)) continue

					tempCol.set(tile.renderCol)
					if (!visionSet.contains(tileHash))
					{
						tempCol.mul(0.5f)
					}

					renderer.queueSpriteWrapper(floor, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal, colour = tempCol)
				}
			}

			renderer.endStatic()
		}

		renderer.begin(deltaTime, offsetx, offsety, ambientLight)

		for (x in xs until xe)
		{
			for (y in ys until ye)
			{
				val tile = world.grid.tryGet(x, y, null) ?: continue
				val wall = tile.wall ?: continue

				val tileHash = tile.hashCode()
				if (!seenSet.contains(tileHash)) continue

				tempCol.set(tile.renderCol)
				if (!visionSet.contains(tileHash))
				{
					tempCol.mul(0.5f)
				}

				renderer.queueSpriteWrapper(wall, x.toFloat(), y.toFloat(), SpaceSlot.WALL.ordinal, colour = tempCol)
			}
		}
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val renderable = entity.renderable()!!.renderable
		val pos = entity.position()!!
		val tile = world.grid.tryGet(pos.position, null) ?: return

		val tileHash = tile.hashCode()
		if (!seenSet.contains(tileHash)) return

		tempCol.set(tile.renderCol)
		if (!visionSet.contains(tileHash))
		{
			if (SpaceSlot.EntityValues.contains(pos.data.slot)) return

			tempCol.mul(0.5f)
		}

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

		renderer.queue(renderable, px, py, pos.data.slot.ordinal, 1, colour = tempCol)

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
				renderer.queue(below, ax, ay, pos.data.slot.ordinal, 0, colour = tempCol)
			}

			for (above in additional.above.values())
			{
				renderer.queue(above, ax, ay, pos.data.slot.ordinal, 2, colour = tempCol)
			}
		}

		drawExtraEntity(entity, deltaTime)
	}

	override fun afterUpdate(deltaTime: Float)
	{
		renderer.end(Statics.stage.batch)
	}

	abstract fun drawExtraEntity(entity: Entity, deltaTime: Float)

	abstract fun getPlayerPosition(): Vector2

	override fun onTurnEntity(entity: Entity)
	{

	}

	var lastClickTile: AbstractTile? = null
	fun getClickTile(screenX: Int, screenY: Int): AbstractTile?
	{
		val playerPos = getPlayerPosition()

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 0.5f

		val mousex = ((screenX - offsetx) / tileSize).toInt()
		val mousey = ((screenY - offsety) / tileSize).toInt()

		val tile = world.grid.tryGet(mousex, mousey, null)
		lastClickTile?.renderCol = Colour.WHITE
		tile?.renderCol = Colour.YELLOW
		lastClickTile = tile

		return tile
	}
}