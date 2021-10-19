package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

@DataClass(name = "Skeleton", global = true)
class SkeletonData : XmlDataClass()
{
	@DataFileReference(allowedFileTypes = "json", basePath = "../assets/")
	lateinit var path: String

	@DataFileReference(resourceType = "AnimationGraph")
	lateinit var animGraph: String

	var scale: Float = 1f

	var colour: Colour? = Colour.WHITE

	//region generated
	override fun load(xmlData: XmlData)
	{
		path = xmlData.get("Path")
		animGraph = xmlData.get("AnimGraph")
		scale = xmlData.getFloat("Scale", 1f)
		colour = AssetManager.tryLoadColour(xmlData.getChildByName("Colour"))
	}
	//endregion
}

class SkeletonRenderable(val skeleton: Skeleton, val state: AnimationState, val graph: AnimationGraph) : Renderable()
{
	val animationGraphState = AnimationGraphState(this, graph)

	override fun doUpdate(delta: Float): Boolean
	{
		state.update(delta)
		graph.update(delta, animationGraphState)

		val complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		return complete
	}

	fun layerAnimation(anim: String)
	{
		val entry = state.setAnimation(1, anim, false)
		entry.alpha = 0.5f
	}

	fun setAnimationState(state: String)
	{
		animationGraphState.setTargetState(state)
	}

	fun setAnimationState(state: String, duration: Float, nextState: String)
	{
		animationGraphState.setTargetState(state)
		animationGraphState.setNextTargetState(nextState, duration)
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

		val renderable = SkeletonRenderable(skeleton, state, graph)
		renderable.colour.set(colour)
		return renderable
	}
}