package com.lyeeedar.ActionSequence.Actions

import com.lyeeedar.ActionSequence.ActionSequenceState
import com.lyeeedar.Components.position
import com.lyeeedar.Components.renderable
import com.lyeeedar.Direction
import com.lyeeedar.Renderables.Animation.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataValue
import com.lyeeedar.Util.XmlData
import java.util.*

@DataClass(category = "Renderable", name = "Anim")
class AnimationAction : AbstractDurationActionSequenceAction()
{
	enum class Animation
	{
		EXPAND,
		SPIN,
		FADE,
		FLASH,
		BUMP
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

	override fun enter(state: ActionSequenceState)
	{
		val source = state.source
		val sourceRenderable = source.get()?.renderable()?.renderable

		if (sourceRenderable != null)
		{
			sourceRenderable.animation = when(anim)
			{
				Animation.EXPAND -> ExpandAnimation.obtain().set(duration, startSize, endSize, oneWay)
				Animation.SPIN -> SpinAnimation.obtain().set(duration, spinAngle)
				Animation.FADE -> AlphaAnimation.obtain().set(duration, startFade, endFade)
				Animation.FLASH -> BlinkAnimation.obtain().set(targetColour, sourceRenderable.colour, duration)
				Animation.BUMP -> BumpAnimation.obtain().set(duration, Direction.getDirection(source.get()!!.position()!!.position, state.targets.get(0)))
			}
		}
	}

	override fun exit(state: ActionSequenceState)
	{

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