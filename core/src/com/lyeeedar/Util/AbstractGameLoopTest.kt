package com.lyeeedar.Util

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.lyeeedar.Game.newGame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class AbstractGameLoopTest(val completionCallback: ()->Unit) {
	val delayMS = 1000L

	fun run() {
		Statics.test = true

		runBlocking  {
			Statics.logger.logDebug("###################################################################")
			Statics.logger.logDebug("Beginning Game Loop Test")
			Statics.logger.logDebug("###################################################################")

			while (Statics.game.currentScreen == null)
			{
				delay(1000)
			}
			delay(delayMS)

			Statics.logger.logDebug("###################################################################")
			Statics.logger.logDebug("Starting new game")
			Statics.logger.logDebug("###################################################################")
			invokeOnMainThread {
				newGame()
			}

			Statics.logger.logDebug("###################################################################")
			Statics.logger.logDebug("Running test")
			Statics.logger.logDebug("###################################################################")
			doRun()

			Statics.logger.logDebug("###################################################################")
			Statics.logger.logDebug("Game Loop Test completed successfully")
			Statics.logger.logDebug("###################################################################")

			delay(2000)
			completionCallback()
		}

		Statics.test = false
	}

	protected abstract suspend fun doRun()

	protected suspend fun testLanguageSelection()
	{
		Statics.logger.logDebug("---------------------------------------------------------------")
		Statics.logger.logDebug("Testing Language Selection")
		Statics.logger.logDebug("")

		waitUntilActorVisible("Language_en")
		getActor("Language_de")!!.click()

		waitUntilActorVisible("languageWarning")

		delay(delayMS)
		getActor("Language_en")!!.click()

		delay(delayMS)
		getActor("Confirm")!!.click()

		delay(delayMS)
		waitUntilActorVisible("Tutorial")
		clickThroughTutorial()

		Statics.logger.logDebug("")
		Statics.logger.logDebug("Language Selection Succeeded")
		Statics.logger.logDebug("---------------------------------------------------------------")
	}

	protected suspend fun clickThroughTutorial()
	{
		while (true)
		{
			delay(delayMS / 10)

			val tutorial = getActor("Tutorial")

			if (tutorial == null)
			{
				break
			}
			else
			{
				tutorial.click()
			}
		}
	}

	protected suspend fun waitUntilVisibleAndClick(name: String)
	{
		waitUntilActorVisible(name)
		delay(delayMS)
		getActor(name)!!.click()
	}

	protected suspend fun waitUntilActorVisible(name: String)
	{
		val start = System.currentTimeMillis()
		while (true)
		{
			if (getActor(name) != null)
			{
				break
			}

			val current = System.currentTimeMillis()
			val diff = current - start

			if (diff > 20000) // 20 seconds
			{
				invokeOnMainThread {
					throw RuntimeException("Widget $name never appeared!")
				}
				throw RuntimeException("Widget $name never appeared!")
			}
		}
	}

	protected suspend fun getActor(name: String): Actor?
	{
		return invokeOnMainThreadAndReturn {
			getAllActors().firstOrNull { it.name?.toLowerCase(Locale.ENGLISH) == name.toLowerCase(Locale.ENGLISH) }
		}
	}

	protected fun getAllActors(): Sequence<Actor>
	{
		return sequence {
			for (actor in Statics.stage.actors)
			{
				if (actor == null) continue

				for (actor in getAllActors(actor))
				{
					yield(actor)
				}
			}
		}
	}

	protected fun getAllActors(actor: Actor): Sequence<Actor>
	{
		return sequence {
			yield(actor)

			if (actor is WidgetGroup)
			{
				for (child in actor.children)
				{
					if (child == null) continue

					for (actor in getAllActors(child))
					{
						yield(actor)
					}
				}
			}
		}
	}
}

suspend fun Actor.click()
{
	invokeOnMainThread {
		val stageCoords = this.localToStageCoordinates(Vector2(this.width / 2f, this.height / 2f))

		val eventDown = InputEvent()
		eventDown.stageX = stageCoords.x
		eventDown.stageY = stageCoords.y
		eventDown.type = InputEvent.Type.touchDown
		this.fire(eventDown)

		val eventUp = InputEvent()
		eventUp.stageX = stageCoords.x
		eventUp.stageY = stageCoords.y
		eventUp.type = InputEvent.Type.touchUp
		this.fire(eventUp)
	}
}

suspend fun invokeOnMainThread(func: ()->Unit)
{
	val blocker = CompletableDeferred<Int>()

	Future.call(
		{
			func()
			blocker.complete(0)
		}, 0f)

	blocker.await()
}

suspend fun <T> invokeOnMainThreadAndReturn(func: () -> T): T
{
	val blocker = CompletableDeferred<T>()

	Future.call(
		{
			blocker.complete(func())
		}, 0f)

	return blocker.await()
}