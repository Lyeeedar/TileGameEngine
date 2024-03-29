package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.lyeeedar.Util.Statics
import org.mockito.Mockito

object AssetTester
{
	@JvmStatic fun main(arg: Array<String>)
	{
		println("##########################################################")
		try
		{
			Statics.test = true
			Gdx.gl = Mockito.mock(GL20::class.java)
			Gdx.gl20 = Mockito.mock(GL20::class.java)
			Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))

			val start = System.currentTimeMillis()

			val funcs = com.badlogic.gdx.utils.Array<Pair<String, ()->Any>>()
			funcs.add(Pair("XmlLoadTest", { XmlLoadTester.test() }))
			funcs.add(Pair("ValidateEnglish", { Localiser().validateEnglish() }))

			addGameAssetTests(funcs)

			val timings = com.badlogic.gdx.utils.Array<Pair<String, Long>>()
			for (func in funcs)
			{
				val start = System.currentTimeMillis()
				func.second.invoke()
				val end = System.currentTimeMillis()
				timings.add(Pair(func.first, end - start))
			}

			val end = System.currentTimeMillis()

			val diff = end - start
			val seconds = diff / 1000

			println("Testing complete in $seconds seconds")

			for (timing in timings)
			{
				println("Completed ${timing.first} in ${timing.second/1000} seconds")
			}
		}
		catch (ex: Exception)
		{
			println("Testing failed!")
			System.err.println(ex.toString())
			throw ex
		}
		finally {
			Gdx.app.exit()
			println("##########################################################")
		}
	}
}

expect fun addGameAssetTests(funcs: com.badlogic.gdx.utils.Array<Pair<String, ()->Any>>)
expect fun addGameLongTests(funcs: com.badlogic.gdx.utils.Array<Pair<String, ()->Any>>)