package com.lyeeedar.UI

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Components.position
import com.lyeeedar.Components.renderable
import com.lyeeedar.Systems.AbstractRenderSystem
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics

class RenderSystemWidget(var world: World<*>) : Widget()
{
	val white = AssetManager.tryLoadTextureRegion("GUI/border")

	var mousePos = Vector2()
	val selectedPoint: Point
		get() = screenspaceToPoint(mousePos.x, mousePos.y)
	var isSelected = false
	var clickConsumed = false

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
				            clickConsumed = false

				            super.touchDown(event, x, y, pointer, button)
				            return true
			            }

			            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean
			            {
				            mousePos.set(x, y)

				            super.mouseMoved(event, x, y)
				            return true
			            }

			            override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int)
			            {
				            mousePos.set(x, y)

				            super.touchDragged(event, x, y, pointer)
			            }

			            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
			            {
				            mousePos.set(x, y)

				            isSelected = false

				            super.touchUp(event, x, y, pointer, button)
			            }
		            })
	}

	private val offsetVec = Vector2()
	fun getPlayerOffset(): Vector2
	{
		val render = world.systems.filterIsInstance<AbstractRenderSystem>().firstOrNull()
		val playerPos = render?.getPlayerPosition(null) ?: Vector2()

		val tileSize = world.tileSize

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f + x
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 2f + y

		offsetVec.set(offsetx, offsety)
		return offsetVec
	}

	val tmp = Vector2()
	fun screenspaceToPoint(x: Float, y: Float): Point
	{
		val stagePos = localToStageCoordinates(tmp.set(x, y))

		val offset = getPlayerOffset()
		val tileSize = world.tileSize

		val xp = stagePos.x - offset.x
		val yp = stagePos.y - offset.y

		val sx = (xp / tileSize).toInt()
		val sy = (yp / tileSize).toInt()

		return Point(sx, sy)
	}

	fun pointToScreenspace(point: Point): Vector2
	{
		return pointToScreenspace(point.xFloat, point.yFloat)
	}

	fun pointToScreenspace(x: Float, y: Float): Vector2
	{
		val offset = getPlayerOffset()
		val tileSize = world.tileSize

		val local = Vector2(offset.x + x * tileSize, offset.y + y * tileSize - this.y)
		return localToStageCoordinates(local)
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
		var instance: RenderSystemWidget? = null
	}
}

class AttachedToEntityWidget(val entity: EntityReference, val widget: Widget)
{
	var lastPos: Vector2 = Vector2()
}