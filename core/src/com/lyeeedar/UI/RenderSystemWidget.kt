package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.lyeeedar.Components.position
import com.lyeeedar.Systems.AbstractTile
import com.lyeeedar.Systems.World
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics

class RenderSystemWidget(val world: World<*>) : Widget()
{
	val white = AssetManager.loadTextureRegion("GUI/border")

	var selectedPoint: Point? = null
	var selectedTile: AbstractTile? = null

	init
	{
		instance = this

		touchable = Touchable.enabled

		addListener(object : InputListener()
		            {
			            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
			            {
				            selectedPoint = screenspaceToPoint(x, y)
				            selectedTile = world.grid.tryGet(selectedPoint!!, null)

				            super.touchDown(event, x, y, pointer, button)
				            return true
			            }

			            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
			            {
				            selectedPoint = null
				            selectedTile = null

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
		val playerPos = world.player!!.position()!!.position
		val tileSize = world.tileSize

		val offsetx = Statics.resolution.x * 0.5f - playerPos.x * tileSize - tileSize * 0.5f
		val offsety = Statics.resolution.y * 0.5f - playerPos.y * tileSize - tileSize * 0.5f

		return this.localToStageCoordinates(Vector2(offsetx + x * world.tileSize, offsety + y * world.tileSize))
	}

	companion object
	{
		lateinit var instance: RenderSystemWidget
	}
}