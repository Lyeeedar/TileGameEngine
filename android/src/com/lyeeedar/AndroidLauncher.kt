package com.lyeeedar

import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.lyeeedar.Screens.GameLoopTest
import com.lyeeedar.Util.Statics

class AndroidLauncher : AndroidApplication()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		val config = AndroidApplicationConfiguration()
		config.resolutionStrategy.calcMeasures(360, 640)
		config.disableAudio = false
		config.useGL30 = true

		Statics.android = true
		Statics.game = MainGame()

		initialize(Statics.game, config)

		Statics.applicationChanger = AndroidApplicationChanger()
		Statics.applicationChanger.updateApplication(Statics.applicationChanger.prefs)

		val launchIntent = intent
		Statics.logger.logDebug("Launch Intent: ${intent.action}")

		if (launchIntent.action == "com.google.intent.action.TEST_LOOP")
		{
			GameLoopTest {
				Gdx.app.exit()
				finish()
				finishAffinity()
				System.exit(0)
			}.run()
		}
	}
}
