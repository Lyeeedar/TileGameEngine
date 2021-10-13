package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.MainGame
import com.lyeeedar.Screens.AbstractScreen
import com.lyeeedar.UI.*
import com.lyeeedar.UI.Tooltip
import ktx.collections.set

class Statics
{
	companion object
	{
		val gameTitle: String by lazy { Localisation.getText("title", "UI") }

		val skin: Skin by lazy { loadSkin() }
		var fps = 60
		var android = false
		var release = false
		var test = false
		val debug: Boolean
			get() = !release

		lateinit var applicationChanger: AbstractApplicationChanger

		var crashReporter: ICrashReporter = DummyCrashReporter()
		var logger: ILogger = DummyLogger()
		var performanceTracer: IPerformanceTracer = DummyPerformanceTracer()
		var analytics: IAnalytics = DummyAnalytics()

		var resolution = Point(360, 640)
		var screenSize = Point(resolution.x, resolution.y)

		var lightCollisionGrid: Array2D<Boolean>? = null

		lateinit var controls: Controls

		lateinit var game: MainGame

		var supportsDiagonals = false

		val stage: Stage
			get() = (game.screen as AbstractScreen).stage

		val debugConsole: DebugConsole
			get() = (game.screen as AbstractScreen).debugConsole

		var settings = Settings()

		var language = "en"

		fun setup()
		{
			controls = Controls()

			Colors.put("IMPORTANT", Color(0.6f, 1f, 0.9f, 1f))
		}
	}
}

class Settings
{
	val data = ObjectMap<String, Any>()

	fun hasKey(key: String) = data.containsKey(key)
	fun <T> get(key: String, default: T) = if (data.containsKey(key)) data[key] as T else default
	fun set(key: String, value: Any) { data[key] = value }

	fun set(other: Settings)
	{
		for (pair in other.data)
		{
			data[pair.key] = pair.value
		}
	}

	fun save(kryo: Kryo, output: Output)
	{
		kryo.writeObject(output, data)
	}

	companion object
	{
		fun load(kryo: Kryo, input: Input): Settings
		{
			val settings = Settings()

			val newData = kryo.readObject(input, ObjectMap::class.java)

			for (pair in newData)
			{
				settings.data[pair.key as String] = pair.value
			}

			return settings
		}
	}
}