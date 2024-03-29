package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Renderer.SortedRenderer
import com.lyeeedar.Renderables.SkeletonRenderable
import com.lyeeedar.SpaceSlot
import com.lyeeedar.SpaceSlotType
import com.lyeeedar.UI.RenderSystemWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.round

abstract class AbstractRenderSystem(world: World<*>) : AbstractEntitySystem(world, world.getEntitiesFor().all(ComponentType.Position, ComponentType.Renderable).get())
{
	val white = AssetManager.tryLoadTextureRegion("Sprites/white.png")!!
	val checkerCol = Colour(0f, 0f, 0f, 0.04f).lockColour()

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
	val shadows = world.getEntitiesFor().all(ComponentType.Position, ComponentType.Shadow).get()

	var doStaticRender = true

	var checkerboard = true

	override fun beforeUpdate(deltaTime: Float)
	{
		val renderer = renderer

		val playerOffset = getPlayerPosition(deltaTime)
		playerOffsetX = playerOffset.x
		playerOffsetY = playerOffset.y

		val screenX = RenderSystemWidget.instance!!.x
		val screenY = RenderSystemWidget.instance!!.y
		val screenWidth = RenderSystemWidget.instance!!.width
		val screenHeight = RenderSystemWidget.instance!!.height

		val screenTileWidth = (Statics.stage.width / tileSize).toInt() + 8
		val screenTileHeight = (Statics.stage.height / tileSize).toInt() + 8

		offsetx = (screenWidth * 0.5f) - (playerOffsetX * tileSize) - (tileSize * 0.5f) + screenX
		offsety = (screenHeight * 0.5f) - (playerOffsetY * tileSize) - (tileSize * 0.5f) + screenY

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

						if (checkerboard && tile.wall == null)
						{
							if ((tile.x + tile.y).rem(2) == 0)
							{
								renderer.queueTexture(white, tile.x.toFloat(), tile.y.toFloat(), SpaceSlot.FLOOR.ordinal, index = 1, colour = checkerCol)
							}
						}
					}
					else
					{
						val clampedTile = world.grid.getClamped(x, y)

						if (clampedTile.floor != null)
						{
							renderer.queueSpriteWrapper(clampedTile.floor!!, x.toFloat(), y.toFloat(), SpaceSlot.FLOOR.ordinal, colour = nonVisibleColour)
						}

						if (tile == null && clampedTile.wall != null)
						{
							renderer.queueSpriteWrapper(clampedTile.wall!!, x.toFloat(), y.toFloat(), SpaceSlot.WALL.ordinal, colour = nonVisibleColour)
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

					drawExtraTile(tile)

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

			val pos = entity.position()!!
			var lx = pos.position.xFloat + 0.5f + pos.offset.x
			var ly = pos.position.yFloat + 0.5f + pos.offset.y

			val renderOffset = entity.renderOffset()
			if (renderOffset != null)
			{
				lx += renderOffset[0]
				ly += renderOffset[1]
			}

			renderer.addLight(light.light, lx, ly)
		}
		for (entity in shadows.entities)
		{
			val shadow = entity.shadow()!!

			val pos = entity.position()!!
			var lx = pos.position.xFloat + 0.5f + pos.offset.x
			var ly = pos.position.yFloat + 0.5f + pos.offset.y

			val renderOffset = entity.renderOffset()
			if (renderOffset != null)
			{
				lx += renderOffset[0]
				ly += renderOffset[1]
			}

			renderer.addShadow(shadow.shadow, lx, ly)
		}
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val renderer = renderer

		val renderable = entity.renderable()!!.renderable
		val pos = entity.position()!!
		val px = pos.position.xFloat + pos.offset.x
		val py = pos.position.yFloat + pos.offset.y

		val tile = world.grid.tryGet(px.round(), py.round(), null)
		if (tile == null)
		{
			if (entity.hasComponent(ComponentType.Transient))
			{
				entity.markForDeletion(0f, "Off map")
			}
			return
		}

		val tileCol = if (entity.renderable()!!.ignoreTileCol) Colour.WHITE else tile.getRenderCol()

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
			if (renderable is SkeletonRenderable)
			{
				renderable.animationGraphState.completeAnimations()
			}

			if (entity.hasComponent(ComponentType.Transient))
			{
				entity.markForDeletion(0f, "Out of Sight")
			}

			if (tile.skipRender || outOfRange || pos.slot.type != SpaceSlotType.MAP) return
		}

		if (renderable is ParticleEffect)
		{

			if (renderable.completed && entity.hasComponent(ComponentType.Transient) && renderable.complete())
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
			renderable.flipX = when (pos.facing)
			{
				Direction.NORTH, Direction.WEST -> true
				else -> false
			}

			renderable.size[0] = pos.size
			renderable.size[1] = pos.size
		}

		if (renderable is SkeletonRenderable)
		{
			val variables = entity.variables()
			if (variables != null)
			{
				renderable.animationGraphState.variables.clear()
				renderable.animationGraphState.variables.putAll(variables.variables)
			}
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
	abstract fun drawExtraTile(tile: AbstractTile)

	abstract fun getPlayerPosition(deltaTime: Float?): Vector2

	override fun onTurnEntity(entity: Entity)
	{

	}

	override fun free()
	{
		cachedRenderer?.dispose()
	}
}