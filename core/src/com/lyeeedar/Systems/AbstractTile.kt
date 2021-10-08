package com.lyeeedar.Systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.ActionSequence.ActionSequenceReference
import com.lyeeedar.Components.EntityReference
import com.lyeeedar.Pathfinding.IPathfindingTile
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.SpaceSlot
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Point

abstract class AbstractTile(x: Int, y: Int) : Point(x, y), IPathfindingTile
{
	var isTileDirty = false

	lateinit var world: World<*>

	var floor: SpriteWrapper? = null
	var wall: SpriteWrapper? = null
	val contents: FastEnumMap<SpaceSlot, EntityReference> = FastEnumMap(SpaceSlot::class.java)

	var bakedLighting: Colour = Colour.WHITE.copy()
	var tileCol: Colour = Colour.WHITE.copy()

	var skipRender: Boolean = false
	var skipRenderEntities: Boolean = true

	val runningSequences = ObjectSet<ActionSequenceReference>()

	val queuedActions = Array<DelayedAction>(false, 4)
	fun addDelayedAction(function: () -> Unit, delay: Float)
	{
		queuedActions.add(DelayedAction.obtain().set(function, delay, this))
	}

	fun tileContainsDelayedAction(): Boolean
	{
		if (queuedActions.size > 0) return true

		if (runningSequences.size > 0)
		{
			var hasSequence = false

			val itr = runningSequences.iterator()
			while (itr.hasNext())
			{
				val stateRef = itr.next()
				val state = stateRef.get()
				if (state == null || state.sequence.isBlocked(state) || state.completed)
				{
					itr.remove()
				}
				else
				{
					hasSequence = true
				}
			}

			return hasSequence
		}

		return false
	}

	abstract fun getRenderCol(): Colour
}

class DelayedAction() : Comparable<DelayedAction>
{
	lateinit var function: () -> Unit
	var delay: Float = 0f
	lateinit var target: AbstractTile

	fun set(function: ()-> Unit, delay: Float, target: AbstractTile): DelayedAction
	{
		this.function = function
		this.delay = delay
		this.target = target

		return this
	}

	override fun compareTo(other: DelayedAction): Int
	{
		return delay.compareTo(other.delay)
	}

	var obtained: Boolean = false
	companion object
	{
		private val pool: Pool<DelayedAction> = object : Pool<DelayedAction>() {
			override fun newObject(): DelayedAction
			{
				return DelayedAction()
			}
		}

		@JvmStatic fun obtain(): DelayedAction
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()

			obj.obtained = true
			return obj
		}
	}
	fun free() { if (obtained) { pool.free(this); obtained = false } }
}