package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.Skeleton

class SkeletonRenderable(val skeleton: Skeleton, val state: AnimationState) : Renderable()
{
	override fun doUpdate(delta: Float): Boolean
	{
		state.update(delta)
		state.apply(skeleton)

		val complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		return complete
	}

	override fun doRender(batch: Batch, x: Float, y: Float, tileSize: Float)
	{
		TODO("Not yet implemented")
	}

	override fun copy(): Renderable
	{
		return SkeletonRenderable(skeleton, state)
	}
}