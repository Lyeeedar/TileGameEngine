package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.Skeleton

class SkeletonRenderable(val skeleton: Skeleton, val state: AnimationState) : Renderable()
{
	var timeInAnimation = -1f

	override fun doUpdate(delta: Float): Boolean
	{
		if (timeInAnimation > 0f)
		{
			timeInAnimation -= delta

			if (timeInAnimation <= 0f)
			{
				state.addAnimation(0, "idle", true, 0f)
			}
		}

		state.update(delta)
		state.apply(skeleton)
		skeleton.updateWorldTransform()

		val complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		return complete
	}

	fun setAnimation(anim: String, duration: Float)
	{
		val current = state.getCurrent(0)
		if (current.animation.name != anim)
		{
			state.setAnimation(0, anim, true)
		}
		timeInAnimation = duration
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