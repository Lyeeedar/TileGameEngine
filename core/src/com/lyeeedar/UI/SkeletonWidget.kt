package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.esotericsoftware.spine.SkeletonRenderer
import com.lyeeedar.Renderables.SkeletonRenderable

class SkeletonWidget(val skeleton: SkeletonRenderable, val originalWidth: Float, val originalHeight: Float) : Widget()
{
	init
	{
		this.width = originalWidth
		this.height = originalHeight
		setFillParent(true)
	}

	private val skeletonRenderer = SkeletonRenderer()

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		validate()

		skeleton.skeleton.setPosition(x + width * 0.5f, y + height * 0.5f)
		skeleton.skeleton.setScale(scaleX, scaleY)
		skeleton.skeleton.color = color
		skeleton.state.apply(skeleton.skeleton)
		skeleton.skeleton.updateWorldTransform()

		skeletonRenderer.draw(batch, skeleton.skeleton)
	}

	override fun getPrefWidth(): Float
	{
		return width
	}

	override fun getPrefHeight(): Float
	{
		return height
	}

	override fun setSize(width: Float, height: Float)
	{
		this.width = width
		this.height = height
	}

	override fun act(delta: Float)
	{
		super.act(delta)
		skeleton.update(delta)
	}
}