package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.renderable
import com.lyeeedar.Renderables.Animation.AlphaAnimation
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.SpinAnimation
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataValue
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Renderable")
class AnimationAction : AbstractDurationActionSequenceAction()
{
	enum class Animation
	{
		EXPAND,
		SPIN,
		FADE,
		FLASH
	}

	lateinit var anim: Animation

	@DataValue(visibleIf = "anim == Expand")
	var startSize: Float = 1f
	@DataValue(visibleIf = "anim == Expand")
	var endSize: Float = 1f
	@DataValue(visibleIf = "anim == Expand")
	var oneWay: Boolean = true

	@DataValue(visibleIf = "anim == Fade")
	var startFade: Float = 1f
	@DataValue(visibleIf = "anim == Fade")
	var endFade: Float = 1f

	@DataValue(visibleIf = "anim == Spin")
	var spinAngle: Float = 0f

	@DataValue(visibleIf = "anim == Flash")
	var targetColour: Colour = Colour.RED

	override fun onTurn(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	override fun enter(state: ActionSequenceState): ActionState
	{
		val source = state.source
		val sourceRenderable = source.renderable()?.renderable

		if (sourceRenderable != null)
		{
			sourceRenderable.animation = when(anim)
			{
				Animation.EXPAND -> ExpandAnimation.obtain().set(duration, startSize, endSize, oneWay)
				Animation.SPIN -> SpinAnimation.obtain().set(duration, spinAngle)
				Animation.FADE -> AlphaAnimation.obtain().set(duration, startFade, endFade)
				Animation.FLASH -> BlinkAnimation.obtain().set(targetColour, sourceRenderable.colour, duration)
			}
		}

		return ActionState.Completed
	}

	override fun exit(state: ActionSequenceState): ActionState
	{
		return ActionState.Completed
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		anim = Animation.valueOf(xmlData.get("Anim").toUpperCase(Locale.ENGLISH))
		startSize = xmlData.getFloat("StartSize", 1f)
		endSize = xmlData.getFloat("EndSize", 1f)
		oneWay = xmlData.getBoolean("OneWay", true)
		startFade = xmlData.getFloat("StartFade", 1f)
		endFade = xmlData.getFloat("EndFade", 1f)
		spinAngle = xmlData.getFloat("SpinAngle", 0f)
		targetColour = AssetManager.tryLoadColour(xmlData.getChildByName("TargetColour"))!!
	}
	override val classID: String = "Animation"
	//endregion
}