package com.lyeeedar.UI

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.lyeeedar.Util.Statics

val longPressDuration = 0.5f

fun Actor.getBounds(): Rectangle
{
	val pos = localToStageCoordinates(Vector2())
	return Rectangle(pos.x, pos.y, this.width, this.height)
}

fun Actor.ensureOnScreen(pad: Float = 0f)
{
	if (width > stage.width - pad*2f)
	{
		width = stage.width - pad*2f
	}

	if (height > stage.height - pad*2f)
	{
		height = stage.height - pad*2f
	}

	// Fit within stage

	if (x < pad)
	{
		x = pad
	}
	else if (x + width > stage.width - pad)
	{
		x = stage.width - width - pad
	}

	if (y < pad)
	{
		y = pad
	}
	else if (y + height > stage.height - pad)
	{
		y = stage.height - height - pad
	}

	setPosition(x, y)
}

fun Actor.addClickListener(func: () -> Unit): Actor
{
	this.touchable = Touchable.enabled
	this.addListener(object : ClickListener() {
		override fun clicked(event: InputEvent?, x: Float, y: Float)
		{
			event?.handle()
			super.clicked(event, x, y)
			func()
		}
	})

	return this
}

fun Actor.addClickListenerFull(func: (InputEvent?, Float, Float) -> Unit)
{
	this.touchable = Touchable.enabled
	this.addListener(object : ClickListener() {
		override fun clicked(event: InputEvent?, x: Float, y: Float)
		{
			event?.handle()
			super.clicked(event, x, y)
			func(event, x, y)
		}
	})
}

fun Actor.addHoldListenerFull(func: (Actor?, Float, Float) -> Unit)
{
	this.touchable = Touchable.enabled
	this.addListener(object : ActorGestureListener(20f, 0.4f, longPressDuration, Int.MAX_VALUE.toFloat()) {
		override fun longPress(actor: Actor?, x: Float, y: Float): Boolean
		{
			super.longPress(actor, x, y)
			func(actor, x, y)
			return true
		}
	})
}


fun Actor.addTapToolTip(provider: () -> String): Actor
{
	this.addClickListenerFull { event, x, y ->
		event?.listenerActor?.showTooltip(provider.invoke(), x, y)
		event?.handle()
	}

	return this
}

fun Actor.addTapToolTip(content: String): Actor
{
	return addTapToolTip { content }
}

fun Actor.addToolTip(title: String, body: String, stage: Stage): Actor
{
	val titleLabel = Label(title, Statics.skin, "title")
	val bodyLabel = Label(body, Statics.skin)

	val table = Table()
	table.add(titleLabel).expandX().center()
	table.row()
	table.add(bodyLabel).expand().fill()

	val tooltip = Tooltip(table, Statics.skin, stage)

	this.addListener(TooltipListener(tooltip))

	return this
}

fun Actor.addHoldToolTip(provider: () -> String): Actor
{
	this.addHoldListenerFull { actor, x, y ->

		actor?.showTooltip(provider.invoke(), x, y)
	}

	return this
}

fun String.showTooltip(event: InputEvent?, x: Float, y: Float)
{
	event!!.listenerActor.showTooltip(this, x, y)
}

fun Actor.showTooltip(content: String, x: Float, y: Float)
{
	val table = Table()
	val label = Label(content, Statics.skin)
	label.wrap = true
	table.add(label).grow().pad(10f).prefWidth(200f).center()

	val tooltip = Tooltip(table, Statics.skin, Statics.stage)

	val fullscreenTable = Table()
	fullscreenTable.touchable = Touchable.enabled
	fullscreenTable.setFillParent(true)
	fullscreenTable.addClickListener {
		tooltip.remove()
		Tooltip.openTooltip = null
		fullscreenTable.remove()
	}
	Statics.stage.addActor(fullscreenTable)

	tooltip.toFront()
	tooltip.show(this, x, y)
}