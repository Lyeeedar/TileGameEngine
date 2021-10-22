package com.lyeeedar.Renderables

import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.spine.Animation
import com.esotericsoftware.spine.AnimationState
import com.esotericsoftware.spine.Slot
import com.lyeeedar.Renderables.AbstractAnimationGraphNode
import com.lyeeedar.Renderables.AnimGraph.AbstractAnimGraphAction
import com.lyeeedar.Util.*
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClassLoader
import ktx.collections.set

class AnimationGraphState(val renderable: SkeletonRenderable, val graph: AnimationGraph)
{
	var current: AbstractAnimationGraphNode? = null
	var currentTargetState: String? = null
	var nextStateTime: Float = 0f
	var nextTargetState: String? = null

	var trackEntry: AnimationState.TrackEntry? = null

	val animationMap = ObjectMap<String, Animation>()

	val variables = ObjectFloatMap<String>()

	val slotCache = ObjectMap<String, Slot>()

	val enteredRootActions = ObjectSet<AbstractAnimGraphAction>()
	val enteredStateActions = ObjectSet<AbstractAnimGraphAction>()

	val actionData = ObjectMap<String, Any>()

	init
	{
		for (node in graph.nodeMap.values())
		{
			val anim = renderable.skeleton.data.findAnimation(node.animation)
			if (anim != null)
			{
				animationMap[node.animation] = anim
			}
		}
	}

	fun getSlot(name: String): Slot
	{
		var slot = slotCache[name]
		if (slot == null)
		{
			slot = renderable.skeleton.findSlot(name)!!
			slotCache[name] = slot
		}

		return slot
	}

	fun setTargetState(state: String)
	{
		if (!graph.root.reachableNodes.containsKey(state)) throw RuntimeException("State $state does not exist in graph")

		currentTargetState = state

		nextTargetState = null
		nextStateTime = -1f
	}

	fun setNextTargetState(state: String, after: Float)
	{
		if (!graph.root.reachableNodes.containsKey(state)) throw RuntimeException("State $state does not exist in graph")
		nextTargetState = state
		nextStateTime = after
	}

	fun transitionTo(node: AbstractAnimationGraphNode)
	{
		for (action in enteredStateActions)
		{
			action.exit(this)
		}
		enteredStateActions.clear()

		current = node
		node.enter(this)
		node.update(0f, this)
	}

	fun completeAnimations()
	{
		if (nextTargetState != null)
		{
			currentTargetState = nextTargetState
			nextStateTime = -1f
		}

		if (currentTargetState != null)
		{
			transitionTo(graph.nodeMap.values().first { it.name == currentTargetState })
			currentTargetState = null
		}
	}
}

@DataFile(colour = "200,255,100", icon = "Sprites/Oryx/uf_split/uf_heroes/wolf_red_1.png")
class AnimationGraph : GraphXmlDataClass<AbstractAnimationGraphNode>()
{
	@DataGraphNodes
	val nodeMap: ObjectMap<String, AbstractAnimationGraphNode> = ObjectMap<String, AbstractAnimationGraphNode>()

	@DataGraphReference
	lateinit var root: AbstractAnimationGraphNode

	val actions: Array<AbstractAnimGraphAction> = Array()

	fun update(delta: Float, state: AnimationGraphState)
	{
		if (state.current == null)
		{
			state.transitionTo(root)
		}

		if (state.nextTargetState != null)
		{
			state.nextStateTime -= delta
			if (state.nextStateTime <= 0f)
			{
				state.setTargetState(state.nextTargetState!!)
			}
		}

		val current = state.current!!
		current.update(delta, state)

		if (state.currentTargetState != null && current is LoopAnimationGraphNode)
		{
			if (current.name != state.currentTargetState)
			{
				val next = current.transitions.minByOrNull {
					it.next.reachableNodes.get(state.currentTargetState, Int.MAX_VALUE)
				}!!.next
				state.transitionTo(next)
			}

			if (state.current?.name == state.currentTargetState)
			{
				state.currentTargetState = null
			}
		}

		updateActions(current.actions, state.enteredStateActions, delta, state)
		updateActions(actions, state.enteredRootActions, delta, state)
	}

	private fun updateActions(actions: Array<AbstractAnimGraphAction>, enteredActions: ObjectSet<AbstractAnimGraphAction>, delta: Float, state: AnimationGraphState)
	{
		for (i in 0 until actions.size)
		{
			val action = actions[i]
			val valid = action.condition.evaluate(state.variables, Random.sharedRandom) != 0f

			if (valid)
			{
				if (!enteredActions.contains(action))
				{
					action.enter(state)
					enteredActions.add(action)
				}
			}
			else
			{
				if (enteredActions.contains(action))
				{
					action.exit(state)
					enteredActions.remove(action)
				}
			}
		}

		if (enteredActions.size > 0)
		{
			for (action in enteredActions)
			{
				action.update(delta, state)
			}
		}
	}

	override fun afterLoad()
	{
		for (node in nodeMap.values())
		{
			node.updateReachableNodes()
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		val nodeMapEl = xmlData.getChildByName("NodeMap")
		if (nodeMapEl != null)
		{
			for (el in nodeMapEl.children)
			{
				val obj = XmlDataClassLoader.loadAbstractAnimationGraphNode(el.get("classID", el.name)!!)
				obj.load(el)
				val guid = el.getAttribute("GUID")
				nodeMap[guid] = obj
			}
		}
		rootGUID = xmlData.get("Root")
		val actionsEl = xmlData.getChildByName("Actions")
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val objactions: AbstractAnimGraphAction
				val objactionsEl = el
				objactions = XmlDataClassLoader.loadAbstractAnimGraphAction(objactionsEl.get("classID", objactionsEl.name)!!)
				objactions.load(objactionsEl)
				actions.add(objactions)
			}
		}
		resolve(nodeMap)
		afterLoad()
	}
	private lateinit var rootGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractAnimationGraphNode>)
	{
		for (item in nodeMap.values())
		{
			item.resolve(nodes)
		}
		root = nodes[rootGUID]!!
	}
	//endregion
}

@DataGraphNode
abstract class AbstractAnimationGraphNode : GraphXmlDataClass<AbstractAnimationGraphNode>()
{
	lateinit var name: String
	lateinit var animation: String

	val actions: Array<AbstractAnimGraphAction> = Array()

	@Transient
	val reachableNodes = ObjectIntMap<String>()

	abstract fun update(delta: Float, state: AnimationGraphState)
	abstract fun enter(state: AnimationGraphState)

	fun updateReachableNodes()
	{
		reachableNodes.clear()
		updateReachableNodes(reachableNodes, 0)
	}

	abstract fun updateReachableNodes(visited: ObjectIntMap<String>, depth: Int)

	//region generated
	override fun load(xmlData: XmlData)
	{
		name = xmlData.get("Name")
		animation = xmlData.get("Animation")
		val actionsEl = xmlData.getChildByName("Actions")
		if (actionsEl != null)
		{
			for (el in actionsEl.children)
			{
				val objactions: AbstractAnimGraphAction
				val objactionsEl = el
				objactions = XmlDataClassLoader.loadAbstractAnimGraphAction(objactionsEl.get("classID", objactionsEl.name)!!)
				objactions.load(objactionsEl)
				actions.add(objactions)
			}
		}
	}
	abstract val classID: String
	override fun resolve(nodes: ObjectMap<String, AbstractAnimationGraphNode>)
	{
	}
	//endregion
}

class Transition : GraphXmlDataClass<AbstractAnimationGraphNode>()
{
	@DataCompiledExpression(default = "1")
	lateinit var condition: CompiledExpression

	@DataGraphReference
	lateinit var next: AbstractAnimationGraphNode

	//region generated
	override fun load(xmlData: XmlData)
	{
		condition = CompiledExpression(xmlData.get("Condition", "1")!!)
		nextGUID = xmlData.get("Next")
	}
	private lateinit var nextGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractAnimationGraphNode>)
	{
		next = nodes[nextGUID]!!
	}
	//endregion
}

class LoopAnimationGraphNode : AbstractAnimationGraphNode()
{
	val transitions: Array<Transition> = Array()
	var randomizeStart: Boolean = false

	override fun updateReachableNodes(visited: ObjectIntMap<String>, depth: Int)
	{
		if (visited.get(name, Int.MAX_VALUE) <= depth) return
		visited.put(name, depth)

		for (next in transitions)
		{
			next.next.updateReachableNodes(visited, depth+1)
		}
	}

	override fun enter(state: AnimationGraphState)
	{
		val animation = state.animationMap.get(animation)
		if (animation != null)
		{
			state.trackEntry = state.renderable.state.setAnimation(0, animation, true)

			if (randomizeStart)
			{
				state.trackEntry!!.trackTime = Random.sharedRandom.nextFloat() * state.trackEntry!!.animationEnd
			}
		}
		else
		{
			state.trackEntry = null
		}
	}

	override fun update(delta: Float, state: AnimationGraphState)
	{

	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		val transitionsEl = xmlData.getChildByName("Transitions")
		if (transitionsEl != null)
		{
			for (el in transitionsEl.children)
			{
				val objtransitions: Transition
				val objtransitionsEl = el
				objtransitions = Transition()
				objtransitions.load(objtransitionsEl)
				transitions.add(objtransitions)
			}
		}
		randomizeStart = xmlData.getBoolean("RandomizeStart", false)
	}
	override val classID: String = "Loop"
	override fun resolve(nodes: ObjectMap<String, AbstractAnimationGraphNode>)
	{
		super.resolve(nodes)
		for (item in transitions)
		{
			item.resolve(nodes)
		}
	}
	//endregion
}

class AnimAnimationGraphNode : AbstractAnimationGraphNode()
{
	@DataGraphReference
	lateinit var next: AbstractAnimationGraphNode

	override fun updateReachableNodes(visited: ObjectIntMap<String>, depth: Int)
	{
		if (visited.get(name, Int.MAX_VALUE) <= depth) return
		visited.put(name, depth)

		next.updateReachableNodes(visited, depth+1)
	}

	override fun enter(state: AnimationGraphState)
	{
		val animation = state.animationMap.get(animation)
		if (animation != null)
		{
			state.trackEntry = state.renderable.state.setAnimation(0, animation, false)
		}
		else
		{
			state.trackEntry = null
		}
	}

	override fun update(delta: Float, state: AnimationGraphState)
	{
		if (state.trackEntry == null)
		{
			state.transitionTo(next)
		}
		else
		{
			val trackEntry = state.trackEntry!!
			if (trackEntry.trackTime >= trackEntry.animationEnd)
			{
				state.transitionTo(next)
			}
		}
	}

	//region generated
	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		nextGUID = xmlData.get("Next")
	}
	override val classID: String = "Anim"
	private lateinit var nextGUID: String
	override fun resolve(nodes: ObjectMap<String, AbstractAnimationGraphNode>)
	{
		super.resolve(nodes)
		next = nodes[nextGUID]!!
	}
	//endregion
}