package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData

@DataClass(name = "Skeleton", global = true)
class SkeletonData : XmlDataClass()
{
	@DataFileReference(allowedFileTypes = "json", basePath = "../assets/")
	lateinit var path: String

	var scale: Float = 1f

	//region generated
	override fun load(xmlData: XmlData)
	{
		path = xmlData.get("Path")
		scale = xmlData.getFloat("Scale", 1f)
	}
	//endregion
}

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

	fun layerAnimation(anim: String)
	{
		val entry = state.setAnimation(1, anim, false)
		entry.alpha = 0.5f
	}

	override fun doRender(batch: Batch, x: Float, y: Float, tileSize: Float)
	{
		TODO("Not yet implemented")
	}

	override fun copy(): Renderable
	{
		val skeletonData = skeleton.data

		val skeleton = Skeleton(skeletonData)
		val stateData = AnimationStateData(skeletonData)
		stateData.defaultMix = 0.1f
		val state = AnimationState(stateData)
		val entry = state.setAnimation(0, "idle", true)
		entry.trackTime = Random.sharedRandom.nextFloat() * entry.animationEnd

		return SkeletonRenderable(skeleton, state)
	}
}