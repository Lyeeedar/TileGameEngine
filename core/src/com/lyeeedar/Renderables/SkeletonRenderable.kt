package com.lyeeedar.Renderables

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.AnimationStateData
import com.esotericsoftware.spine.Skeleton
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData

@DataClass(name = "Skeleton")
@DataFile(colour = "170,170,150", icon="Sprites/Oryx/uf_split/uf_heroes/skeleton_1.png")
class SkeletonData : XmlDataClass()
{
	@DataFileReference(allowedFileTypes = "json")
	lateinit var path: String

	@DataFileReference(resourceType = "AnimationGraph")
	lateinit var animGraph: String

	var scale: Float = 1f

	var colour: Colour? = Colour.WHITE

	var skin: String = "default"

	//region generated
	override fun load(xmlData: XmlData)
	{
		path = xmlData.get("Path")
		animGraph = xmlData.get("AnimGraph")
		scale = xmlData.getFloat("Scale", 1f)
		colour = AssetManager.tryLoadColour(xmlData.getChildByName("Colour"))
		skin = xmlData.get("Skin", "default")!!
	}
	//endregion
}

class SkeletonRenderable(val skeleton: Skeleton, val state: AnimationState, val graph: AnimationGraph) : Renderable()
{
	val animationGraphState = AnimationGraphState(this, graph)

	val attachedSkeletons = Array<SkeletonRenderable>()

	override fun doUpdate(delta: Float): Boolean
	{
		state.update(delta)
		graph.update(delta, animationGraphState)

		for (i in 0 until attachedSkeletons.size)
		{
			val renderable = attachedSkeletons[i]
			renderable.animationGraphState.variables.clear()
			renderable.animationGraphState.variables.putAll(animationGraphState.variables)
			renderable.update(delta)
		}

		var complete = animation?.update(delta) ?: true
		if (complete)
		{
			animation?.free()
			animation = null
		}

		complete = complete && animationGraphState.currentTargetState == null

		return complete
	}

	fun layerAnimation(anim: String)
	{
		val entry = state.setAnimation(1, anim, false)
		entry.alpha = 0.5f
	}

	fun gotoState(state: String)
	{
		animationGraphState.setTargetState(state)
	}

	fun goFromStateToState(from: String, to: String)
	{
		if (animationGraphState.current?.name == from)
		{
			animationGraphState.setTargetState(to)
		}
	}

	fun gotoStateForDuration(state: String, duration: Float, nextState: String)
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
		skeleton.setSkin(this.skeleton.skin.name)

		val renderable = SkeletonRenderable(skeleton, state, graph)
		renderable.colour.set(colour)
		renderable.light = light?.copy()
		renderable.shadow = shadow?.copy()

		return renderable
	}
}