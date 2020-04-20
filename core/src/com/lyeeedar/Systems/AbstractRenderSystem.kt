package com.lyeeedar.Systems

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.ciel

abstract class AbstractRenderSystem(world: World) : AbstractEntitySystem(world, EntitySignature().all(ComponentType.Position, ComponentType.Renderable))
{
	val tileSize: Float
		get() = world.tileSize

	protected val ambientLight = Colour.WHITE
	protected val batch: SpriteBatch by lazy { SpriteBatch() }
	protected val renderer: SortedRenderer by lazy { SortedRenderer(tileSize, Statics.resolution[0] / tileSize, Statics.resolution[1] / tileSize, SpaceSlot.Values.size, false) }

	protected var playerOffsetX: Float = 0f
	protected var playerOffsetY: Float = 0f
	protected var offsetx: Float = 0f
	protected var offsety: Float = 0f

	protected val outOfSightCutoff = 50f*50f

	protected val hp_dr: Sprite = AssetManager.loadSprite("GUI/health_DR")
	protected val hp_damaged: Sprite = AssetManager.loadSprite("GUI/health_damaged")
	protected val hp_empty: Sprite = AssetManager.loadSprite("GUI/health_empty")

	override fun beforeUpdate(deltaTime: Float)
	{
		val playerOffset = getPlayerPosition()
		playerOffsetX = playerOffset.x
		playerOffsetY = playerOffset.y

		offsetx = (Statics.resolution.x * 0.5f) - (playerOffsetX * tileSize) - (tileSize * 0.5f)
		offsety = (Statics.resolution.y * 0.5f) - (playerOffsetY * tileSize) - (tileSize * 0.5f)

		renderer.begin(deltaTime, offsetx, offsety, ambientLight)
	}

	override fun updateEntity(entity: Entity, deltaTime: Float)
	{
		val renderable = entity.renderable()!!.renderable
		val pos = entity.pos()!!

		val px = pos.position.x.toFloat()
		val py = pos.position.y.toFloat()

		if (Vector2.dst2(px, py, playerOffsetX, playerOffsetY) > outOfSightCutoff)
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

	fun drawHPBar(space: Float, currentHp: Float, lostHP: Int, maxHp: Int, immune: Boolean, xi: Float, yi: Float, hp_full: Sprite): Float
	{
		// do hp bar
		val solidSpaceRatio = 0.12f // 20% free space

		val maxSingleLineHP = space * 15

		val pips = maxHp

		var lines = 1
		var pipsPerLine = pips
		if (pips > maxSingleLineHP)
		{
			lines = 2
			pipsPerLine = pips / 2
		}

		val spacePerPip = space / pipsPerLine.toFloat()
		val spacing = spacePerPip * solidSpaceRatio
		val solid = spacePerPip - spacing

		val hp = currentHp.ciel()
		for (i in 0 until pips)
		{
			val sprite = when {
				immune -> hp_dr
				i < hp -> hp_full
				i < hp + lostHP -> hp_damaged
				else -> hp_empty
			}

			val y = if (i < pipsPerLine && lines > 1) yi+0.25f else yi+0.1f
			val x = if (i >= pipsPerLine && lines > 1) xi+(i-pipsPerLine)*spacePerPip else xi+i*spacePerPip

			val sortY = if (hp == maxHp) null else y.toInt()-2
			renderer.queueSprite(sprite, x, y, SpaceSlot.EFFECT.ordinal, 2, width = solid, height = 0.15f, sortY = sortY)
		}

		if (lines > 1)
		{
			return yi+0.35f
		}
		else
		{
			return yi+0.25f
		}
	}
}