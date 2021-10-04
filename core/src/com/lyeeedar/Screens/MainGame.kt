package com.lyeeedar

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.lyeeedar.Game.load
import com.lyeeedar.Game.newGame
import com.lyeeedar.Screens.AbstractScreen
import com.lyeeedar.Screens.ScreenEnum
import com.lyeeedar.Screens.registerDebugScreens
import com.lyeeedar.Screens.registerGameScreens
import com.lyeeedar.Util.Statics
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane

class MainGame(val debugScreenOverride: ScreenEnum? = null) : Game()
{
	private val screens = HashMap<ScreenEnum, AbstractScreen>()
	private val debugScreens = HashMap<ScreenEnum, AbstractScreen>()
	var currentScreen: AbstractScreen? = null
	val currentScreenEnum: ScreenEnum
		get()
		{
			for (se in ScreenEnum.values())
			{
				if (screens[se] == currentScreen)
				{
					return se
				}
				else if (Statics.debug)
				{
					if (debugScreens[se] == currentScreen)
					{
						return se
					}
				}
			}
			return ScreenEnum.INVALID
		}

	override fun create()
	{
		if (Statics.debug)
		{
			val profiler = GLProfiler(Gdx.graphics)
			profiler.enable()

			profiler.setListener {
				throw java.lang.RuntimeException("Error code: $it")
			}
		}

		Statics.analytics.appOpen()

		Statics.setup()

		if (Statics.android)
		{

		}
		else if (Statics.release)
		{
			val sw = StringWriter()
			val pw = PrintWriter(sw)

			val handler = Thread.UncaughtExceptionHandler { myThread, e ->
				e.printStackTrace(pw)
				val exceptionAsString = sw.toString()

				val file = Gdx.files.local("error.log")
				file.writeString(exceptionAsString, false)

				JOptionPane.showMessageDialog(null, "A fatal error occurred. Please send the error.log to me so that I can fix it.", "An error occurred", JOptionPane.ERROR_MESSAGE)

				e.printStackTrace()
			}

			Thread.currentThread().uncaughtExceptionHandler = handler
		}

		if (Statics.debug)
		{
			debugScreens.putAll(registerDebugScreens())
		}
		screens.putAll(registerGameScreens())

		if (debugScreenOverride != null)
		{
			switchScreen(debugScreenOverride)
		}
		else
		{
			val success = load()

			if (!success)
			{
				newGame()
			}
		}

	}

	fun switchScreen(screen: AbstractScreen)
	{
		this.setScreen(screen)
	}

	fun switchScreen(screen: ScreenEnum)
	{
		var s: AbstractScreen? = screens[screen]
		if (s == null && Statics.debug)
		{
			s = debugScreens[screen]
		}

		if (s != null)
		{
			this.setScreen(s)
		}
	}

	inline fun <reified T : AbstractScreen> getTypedScreen(): T?
	{
		for (screen in getAllScreens())
		{
			if (screen is T)
			{
				return screen
			}
		}

		return null
	}

	fun getAllScreens() = screens.values

	override fun setScreen(screen: Screen?)
	{
		if (currentScreen != null)
		{
			val ascreen = screen as AbstractScreen
			currentScreen = ascreen
			super.setScreen(screen)
		}
		else
		{
			currentScreen = screen as? AbstractScreen
			super.setScreen(screen)
		}
	}

	fun getScreen(screen: ScreenEnum): AbstractScreen = screens[screen]!!
}