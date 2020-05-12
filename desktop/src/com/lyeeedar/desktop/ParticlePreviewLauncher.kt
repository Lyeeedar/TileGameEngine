package com.lyeeedar.desktop

import com.lyeeedar.MainGame
import com.lyeeedar.ScreenEnum
import com.lyeeedar.Util.Statics

object ParticlePreviewLauncher
{
	@JvmStatic fun main(arg: Array<String>)
	{
		//Global.release = true
		Statics.game = MainGame(ScreenEnum.PARTICLEEDITOR)
		Statics.applicationChanger = LwjglApplicationChanger()
		Statics.applicationChanger.createApplication()
	}
}
