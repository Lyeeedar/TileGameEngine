package com.lyeeedar.UI

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics
import ktx.actors.setKeyboardFocus
import ktx.collections.set
import ktx.collections.toGdxArray
import java.util.*


class DebugConsole(val historyKey: String) : Table()
{
	var log: Table
	var text: TextField
	var typedText = ""
	var tabIndex = 0
	var historyIndex = -1
	var showingHistory = false
	lateinit var scroll: ScrollPane

	val history = Statics.settings.get("CommandHistory" + historyKey, Array<String>())
	val commands = ObjectMap<String, ConsoleCommand>()

	init
	{
		log = Table()

		val closeButton = Button(Statics.skin, "closecard")
		closeButton.addClickListener { this@DebugConsole.isVisible = false }
		log.add(closeButton).size(24f).expandX().right()
		log.row()

		log.add(Table()).grow()
		log.row()

		text = object : TextField("", Statics.skin, "console")
		{
			val obj = this

			override fun createInputListener(): InputListener
			{
				return object : TextField.TextFieldClickListener()
				{
					override fun keyDown(event: InputEvent?, keycode: Int): Boolean
					{
						if (keycode == Input.Keys.GRAVE)
						{
							if (!isVisible)
							{
								this@DebugConsole.isVisible = true
							}
							else
							{
								this@DebugConsole.isVisible = false
								obj.setKeyboardFocus(false)
							}

							return true
						}

						return super.keyDown(event, keycode)
					}

					override fun keyUp(event: InputEvent?, keycode: Int): Boolean
					{
						if (keycode == Input.Keys.UP && history.size > 0)
						{
							if (historyIndex == -1)
							{
								historyIndex = history.size-1
							}
							else if (showingHistory)
							{
								historyIndex--
								if (historyIndex < 0) historyIndex = 0
							}
							else
							{
								showingHistory = true
							}

							obj.setText(history[historyIndex])
							obj.setCursorPosition(obj.text.length)

							return true
						}
						else if (keycode == Input.Keys.DOWN && history.size > 0)
						{
							if (historyIndex != -1)
							{
								historyIndex++

								if (historyIndex == history.size)
								{
									historyIndex = -1
								}

								if (historyIndex == -1)
								{
									obj.setText(typedText)
								}
								else
								{
									obj.setText(history[historyIndex])
								}
								obj.setCursorPosition(obj.text.length)
							}

							return true
						}

						return super.keyUp(event, keycode)
					}
				}
			}
		}
		text.setFocusTraversal(false)
		text.setTextFieldListener { textField, key ->
			if (key == '\r' || key == '\n')
			{
				tabIndex = 0

				val commandName = text.text.split(' ').first().lowercase(Locale.ENGLISH)

				if (commandName == "?" || commandName == "help")
				{
					for (command in commands)
					{
						write(command.key)
					}
				} else if (commands.containsKey(commandName))
				{
					val command = commands[commandName]
					val success = command.execute(text.text, this)

					if (success)
					{
						if (history.size == 0 || history.last() != text.text)
						{
							history.add(text.text)
							Statics.settings.set("CommandHistory$historyKey", history)
						}
					} else
					{
						error("Failed to run command '${text.text}'!")
					}
				} else
				{
					error("No command registered with name '$commandName'!")
				}

				typedText = ""
				text.text = ""
				showingHistory = false
			}
			else if (key == '\t')
			{
				val validNames = commands.keys().filter { it.contains(typedText.lowercase(Locale.ENGLISH)) }
				if (tabIndex >= validNames.size)
				{
					tabIndex = 0
					text.text = typedText
				}
				else
				{
					text.text = validNames[tabIndex]
				}

				text.cursorPosition = text.text.length

				tabIndex++
			}
			else
			{
				typedText = text.text
				tabIndex = 0
			}
		}
		text.textFieldFilter = TextField.TextFieldFilter { textField, c -> c != '`' }

		log.background = TextureRegionDrawable(AssetManager.tryLoadTextureRegion("Sprites/white.png")).tint(Color(0.2f, 0.2f, 0.2f, 0.6f))

		scroll = ScrollPane(log, Statics.skin)
		scroll.setScrollingDisabled(true, false)

		log.height = 300f

		this.add(scroll).growX().height(300f)
		this.row()
		this.add(text).growX()

		width = 300f
		height = 300f

		register("clear", "Clear all lines from the console", fun (args, console): Boolean {

			log.clear()

			return true
		})
	}

	fun write(line: String) = writeLine(line, Color.WHITE)

	fun error(line: String) = writeLine(line, Color.RED)

	fun warning(line: String) = writeLine(line, Color.ORANGE)

	private fun writeLine(line: String, col: Color)
	{
		val label = Label(line, Statics.skin, "console")
		label.setWrap(true)
		label.color = col

		log.row()
		log.add(label).width(Value.percentWidth(1f, log)).left().bottom()

		scroll.scrollTo(0f, 0f, 0f, 0f)

		println("Console: " + line)
	}

	fun reregister(name: String, help: String, callback: (args: kotlin.Array<String>, console: DebugConsole) -> Boolean)
	{
		if (Statics.release) return

		val lname = name.lowercase(Locale.ENGLISH)

		commands[lname] = ConsoleCommand(name, help, callback)
	}

	fun register(name: String, help: String, callback: (args: kotlin.Array<String>, console: DebugConsole) -> Boolean)
	{
		if (Statics.release) return

		val lname = name.lowercase(Locale.ENGLISH)
		if (commands.containsKey(lname)) throw Exception("Console command already registered with name '$name'!")

		commands[lname] = ConsoleCommand(name, help, callback)
	}

	fun unregister(name: String)
	{
		if (Statics.release) return

		val lname = name.lowercase(Locale.ENGLISH)

		commands.remove(lname)
	}
}

class ConsoleCommand(val text: String, val help: String, val callback: (args: kotlin.Array<String>, console: DebugConsole) -> Boolean)
{
	fun execute(text: String, console: DebugConsole): Boolean
	{
		println("Command: " + text)

		val argText = text.subSequence(this.text.length, text.length).toString().trim()

		if (argText.lowercase(Locale.ENGLISH) == "help")
		{
			console.write(help)
			return true
		} else
		{
			val args: kotlin.Array<String>

			if (argText.contains('"'))
			{
				val groups = argText.split('"')

				val argList = Array<String>()

				var skip = argText.startsWith('"')
				var currentIsGroup = skip
				for (group in groups)
				{
					if (skip)
					{
						skip = false
						continue
					}

					if (currentIsGroup) argList.add(group)
					else
					{
						val split = group.split(' ')
						argList.addAll(split.toGdxArray())
					}

					currentIsGroup = !currentIsGroup
				}

				args = argList.filter { !it.isBlank() }.asIterable().toList().toTypedArray()
			} else
			{
				args = argText.split(' ').toTypedArray()
			}

			try
			{
				return callback.invoke(args, console)
			} catch (ex: Exception)
			{
				console.error(ex.message ?: "Unknown exception! $ex")

				return false
			}
		}
	}
}

interface IDebugCommandProvider
{
	fun attachCommands(debugConsole: DebugConsole)
	fun detachCommands(debugConsole: DebugConsole)
}