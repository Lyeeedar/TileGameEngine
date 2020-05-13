package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.ShadowCastCache
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.SpaceSlot
import com.lyeeedar.SpaceSlotType
import com.lyeeedar.Util.*
import java.awt.Color

abstract class AbstractRenderSystem(world: World<*>) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.Renderable).get())
{
	val shape: ShapeRenderer by lazy { ShapeRenderer() }
	var drawParticleDebug = false
	var drawEmitters = true
	var drawParticles = true
	var drawEffectors = false
	val particles = Array<ParticleEffect>()

	val tileSize: Float
		get() = world.tileSize

	val renderer: SortedRenderer
		get()
		{
			val cached = cachedRenderer
			if (cached == null || cached.tileSize != world.tileSize || cached.width.toInt() != world.grid.width || cached.height.toInt() != world.grid.height)
			{
				cachedRenderer?.dispose()
				cachedRenderer = SortedRenderer(tileSize, world.grid.width.toFloat(), world.grid.height.toFloat(), SpaceSlot.Values.size, false)
			}

			return cachedRenderer!!
		}
	private var cachedRenderer: SortedRenderer? = null

	protected var playerOffsetX: Float = 0f
	protected var playerOffsetY: Float = 0f
	protected var offsetx: Float = 0f
	protected var offsety: Float = 0f

	private var renderedStaticOffsetX: Float = -10000f
	private var renderedStaticOffsetY: Float = -10000f

	private val nonVisibleBrightness = 0.2f
	private val nonVisibleColour = Colour(nonVisibleBrightness, nonVisibleBrightness, nonVisibleBrightness, 1f, true)

	val lights = world.getEntitiesFor().all(ComponentType.Position, ComponentType.Light).get()

	var doStaticRender = true

	override fun beforeUpdate(deltaTime: Float)
	{
		val renderer = renderer

		val playerOffset = getPlayerPosition(deltaTime)
		playerOffsetX = playerOffset.x
		playerOffsetY = playerOffset.y

		val screenTileWidth = (Statics.resolution.xFloat / tileSize).toInt() + 4
		val screenTileHeight = (Statics.resolution.yFloat / tileSize).toInt() + 4

		offsetx = (Statics.resolution.x * 0.5f) - (playerOffsetX * tileSize) - (tileSize * 0.5f)
		offsety = (Statics.resolution.y * 0.5f) - (playerOffsetY * tileSize) - (tileSize * 0.5f)

		val xs = playerOffsetX.toInt()-screenTileWidth/2
		val xe = playerOffsetX.toInt()+screenTileWidth/2
		val ys = playerOffsetY.toInt()-screenTileHeight/2
		val ye = playerOffsetY.toInt()+screenTileHeight/2

		var isMapDirty = false
		outer@for (x in xs until xe)
		{
			for (y in ys until ye)
			{
				val tile = world.grid.tryGet(x, y, null) ?: continue
				if (tile.isTileDirty)
				{
					isMapDirty = true
					break@outer
				}
			}
		}

		if (doStaticRender && (isMapDirty || renderedStaticOffsetX != offsetx || renderedStaticOffsetY != offsety))
		{
			renderedStaticOffsetX = offsetx
			renderedStaticOffsetY = offsety

			renderer.beginStatic(offsetx, offsety, world.ambientLight)

			for (x in xs until xe)
			{
				for (y in ys until ye)
				{
					val tile = world.grid.tryGet(x, y, null)
					if (tile != null && tile.skipRender == false)
					{
						tile.isTileDirty = false

						val floor = tile.floor ?: continue

						renderer.queueSpriteWrapper(floor, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal, colour = tile.getRenderCol())
					}
					else
					{
						val cornertile = world.grid[0, 0]

						if (cornertile.floor != null)
						{
							renderer.queueSpriteWrapper(cornertile.floor!!, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal, colour = nonVisibleColour)
						}

						if (tile == null && cornertile.wall != null)
						{
							renderer.queueSpriteWrapper(cornertile.wall!!, x.toFloat(), y.toFloat(), SpaceSlot.WALL.ordinal, colour = nonVisibleColour)
						}
					}
				}
			}

			renderer.endStatic()
		}

		renderer.begin(deltaTime, offsetx, offsety, world.ambientLight)

		for (x in xs until xe)
		{
			for (y in ys until ye)
			{
				val tile = world.grid.tryGet(x, y, null)

				if (tile != null)
				{
					if (!doStaticRender)
					{
						val floor = tile.floor
						if (floor != null) renderer.queueSpriteWrapper(floor, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal, colour = tile.getRenderCol())
					}

					if (!tile.skipRender)
					{
						val wall = tile.wall ?: continue
						renderer.queueSpriteWrapper(wall, x.toFloat(), y.toFloat(), SpaceSlot.WALL.ordinal, colour = tile.getRenderCol())
					}
					else
					{
						val cornertile = world.grid[0, 0]
						if (cornertile.wall != null)
						{
							renderer.queueSpriteWrapper(cornertile.wall!!, x.toFloat(), y.toFloat(), SpaceSlot.WALL.ordinal, colour = nonVisibleColour)
						}
					}
				}
			}
		}

		if (drawParticleDebug)
		{
			particles.clear()
		}

		for (entity in lights.entities)
		{
			val light = entity.light()!!

			val pos = entity.position()!!.position
			var lx = pos.xFloat + 0.5f
			var ly = pos.yFloat + 0.5f

			val renderOffset = entity.renderOffset()
			if (renderOffset != null)
			{
				lx += renderOffset[0]
				ly += renderOffset[1]
			}

			renderer.addLight(light.light, lx, ly)
		}
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val renderer = renderer

		val renderable = entity.renderable()!!.renderable
		val pos = entity.position()!!
		val px = pos.position.xFloat + pos.offset.x
		val py = pos.position.yFloat + pos.offset.y

		val tile = world.grid.tryGet(px.round(), py.round(), null) ?: return
		val tileCol = tile.getRenderCol()

		val tilesHeight = Statics.resolution[1] / tileSize
		val outOfRange = pos.position.dist(playerOffsetX.toInt(), playerOffsetY.toInt()) > tilesHeight
		if (tile.skipRender || tile.skipRenderEntities || outOfRange)
		{
			if (renderable.animation != null)
			{
				renderable.animation = null
				if (renderable is ParticleEffect && renderable.killOnAnimComplete)
				{
					renderable.stop()
				}
			}

			if (entity.components.containsKey(ComponentType.Transient))
			{
				entity.markForDeletion(0f, "Out of Sight")
			}

			if (tile.skipRender || outOfRange || pos.slot.type != SpaceSlotType.MAP) return
		}

		if (renderable is ParticleEffect)
		{
			if (renderable.completed && entity.components.containsKey(ComponentType.Transient) && renderable.complete())
			{
				entity.markForDeletion(0f, "completed")
			}

			if (drawParticleDebug)
			{
				particles.add(renderable)
			}
		}
		else
		{
			renderable.size[0] = pos.size
			renderable.size[1] = pos.size
		}

		renderer.queue(renderable, px, py, pos.slot.ordinal, 1, colour = tileCol)

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
				renderer.queue(below, ax, ay, pos.slot.ordinal, 0, colour = tileCol)

				if (drawParticleDebug && below is ParticleEffect)
				{
					particles.add(below)
				}
			}

			for (above in additional.above.values())
			{
				renderer.queue(above, ax, ay, pos.slot.ordinal, 2, colour = tileCol)

				if (drawParticleDebug && above is ParticleEffect)
				{
					particles.add(above)
				}
			}
		}

		drawExtraEntity(entity, deltaTime)
	}

	override fun afterUpdate(deltaTime: Float)
	{
		renderer.end(Statics.stage.batch)

		if (drawParticleDebug)
		{
			Statics.stage.batch.begin()

			shape.projectionMatrix = Statics.stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			for (particle in particles)
			{
				particle.debug(shape, offsetx, offsety, tileSize, drawEmitters, drawParticles, drawEffectors)
			}

			shape.end()

			particles.clear()

			Statics.stage.batch.end()
		}
	}

	abstract fun drawExtraEntity(entity: Entity, deltaTime: Float)

	abstract fun getPlayerPosition(deltaTime: Float): Vector2

	override fun onTurnEntity(entity: Entity)
	{

	}

	override fun free()
	{
		cachedRenderer?.dispose()
	}
}