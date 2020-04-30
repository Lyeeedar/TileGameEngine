package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Components.position
import com.lyeeedar.Components.renderOffset
import com.lyeeedar.Components.renderable
import com.lyeeedar.Systems.AbstractTile
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics

class RenderSystemWidget(val world: World<*>) : Widget()
{
	val white = AssetManager.loadTextureRegion("GUI/border")

	var mousePos = Vector2()
	val selectedPoint: Point
		get() = screenspaceToPoint(mousePos.x, mousePos.y)
	var isSelected = false

	var attachedToEntityWidgets = Array<AttachedToEntityWidget>(false, 4)

	init
	{
		instance = this

		touchable = Touchable.enabled

		addListener(object : InputListener()
		            {
			            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
			            {
				            mousePos.set(x, y)
				            isSelected = true

				            super.touchDown(event, x, y, pointer, button)
				            return true
			            }

			            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
			            {
				            mousePos.set(x, y)

				            isSelected = false

				            super.touchUp(event, x, y, pointer, button)
			            }
		            })
	}

	fun screenspaceToPoint(x: Float, y: Float): Point
	{
		val playerPos = world.player!!.position()!!.position
		val tileSize = world.tileSize

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 0.5f

		val xp = x - offsetx
		val yp = y - offsety

		val sx = (xp / world.tileSize).toInt()
		val sy = (yp / world.tileSize).toInt()

		return Point(sx, sy)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		return pointToScreenspace(point.x.toFloat(), point.y.toFloat())
	}

	fun pointToScreenspace(x: Float, y: Float): Vector2
	{
		val playerPos = world.player!!.position()!!.position.toVec()
		val renderOffset = world.player?.renderable()?.renderable?.animation?.renderOffset(true)
		if (renderOffset != null)
		{
			playerPos.add(renderOffset[0], renderOffset[1])
		}

		val tileSize = world.tileSize

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 0.5f

		return this.localToStageCoordinates(Vector2(offsetx + x * world.tileSize, offsety + y * world.tileSize))
	}

	fun addAttachedToEntityWidget(entity: EntityReference, widget: Widget)
	{
		val holder = AttachedToEntityWidget(entity, widget)
		attachedToEntityWidgets.add(holder)
		update(holder)
	}

	fun update(attachedToEntityWidget: AttachedToEntityWidget): Boolean
	{
		if (attachedToEntityWidget.widget.stage == null) return true
		val entity = attachedToEntityWidget.entity.get() ?: return true

		val dx = attachedToEntityWidget.widget.x - attachedToEntityWidget.lastPos.x
		val dy = attachedToEntityWidget.widget.y - attachedToEntityWidget.lastPos.y

		val entityPos = entity.position()!!.position.toVec()
		val renderOffset = entity.renderable()?.renderable?.animation?.renderOffset(true)
		if (renderOffset != null)
		{
			entityPos.add(renderOffset[0], renderOffset[1])
		}

		val newPos = pointToScreenspace(entityPos.x, entityPos.y)
		attachedToEntityWidget.lastPos.set(newPos)

		attachedToEntityWidget.widget.x = newPos.x + dx
		attachedToEntityWidget.widget.y = newPos.y + dy

		return false
	}

	override fun act(delta: Float)
	{
		val itr = attachedToEntityWidgets.iterator()
		while (itr.hasNext())
		{
			val holder = itr.next()
			val shouldRemove = update(holder)

			if (shouldRemove)
			{
				itr.remove()
			}
		}

		super.act(delta)
	}

	companion object
	{
		lateinit var instance: RenderSystemWidget
	}
}

class AttachedToEntityWidget(val entity: EntityReference, val widget: Widget)
{
	var lastPos: Vector2 = Vector2()
}