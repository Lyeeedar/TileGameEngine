package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Animation.AlphaAnimation
import com.lyeeedar.Renderables.Animation.ColourChangeAnimation
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData
import ktx.collections.set

@DataClass(category = "Renderable")
class ReplaceSourceRenderableAction : AbstractDurationActionSequenceAction()
{
	lateinit var renderable: Renderable
	var blendDuration: Float = 0f
	var restoreOriginal: Boolean = true

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		val source = state.source.get()!!

		var srcRenderable = source.renderable()
		if (srcRenderable == null)
		{
			srcRenderable = source.addComponent(ComponentType.Renderable) as RenderableComponent
			srcRenderable.set(AssetManager.loadSprite("blank"))
		}

		val oldRenderable = srcRenderable.renderable
		val newRenderable = renderable.copy()
		srcRenderable.renderable = newRenderable

		if (blendDuration > 0f)
		{
			val addRenderable = source.addOrGet(ComponentType.AdditionalRenderable) as AdditionalRenderableComponent

			addRenderable.above["blendTo"] = oldRenderable
			oldRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 1f, 0f)
			oldRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE, Colour.WHITE.copy().mul(50f), blendDuration)
			Future.call({ addRenderable.above.remove("blendTo") }, blendDuration * 0.98f)

			newRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 0f, 1f)
			newRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE.copy().mul(50f), Colour.WHITE, blendDuration)
		}

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		if (restoreOriginal)
		{
			val source = state.source.get()!!

			val replacementRenderable = source.renderable()!!.renderable
			source.renderable()!!.reset()
			val originalRenderable = source.renderable()!!.renderable

			if (blendDuration > 0f)
			{
				val addRenderable = source.addOrGet(ComponentType.AdditionalRenderable) as AdditionalRenderableComponent

				addRenderable.above["blendFrom"] = replacementRenderable
				replacementRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 1f, 0f)
				replacementRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE, Colour.WHITE.copy().mul(50f), blendDuration)
				Future.call({ addRenderable.above.remove("blendFrom") }, blendDuration*0.95f)

				originalRenderable.animation = AlphaAnimation.obtain().set(blendDuration, 0f, 1f)
				originalRenderable.animation = ColourChangeAnimation.obtain().set(Colour.WHITE.copy().mul(50f), Colour.WHITE, blendDuration)
			}
		}

		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		renderable = AssetManager.loadRenderable(xmlData.getChildByName("Renderable")!!)
		blendDuration = xmlData.getFloat("BlendDuration", 0f)
		restoreOriginal = xmlData.getBoolean("RestoreOriginal", true)
	}
	override val classID: String = "ReplaceSourceRenderable"
	//endregion
}