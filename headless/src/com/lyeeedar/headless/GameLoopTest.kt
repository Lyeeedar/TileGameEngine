package com.lyeeedar.headless

import java.io.File
import java.io.FileInputStream
import java.util.*

object GameLoopTest
{
	@JvmStatic fun String.runCommand(): String {
		println(this)
		val output = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
		println(output)
		return output
	}

	fun readLogs(androidHome: String, pid: String, completeLog: StringBuilder)
	{
		val logs = "$androidHome/adb logcat -d".runCommand().split('\n')
		for (log in logs)
		{
			completeLog.append(log)
			if (log.contains(" $pid ")) println(log)
		}

		"$androidHome/adb logcat -c".runCommand()
	}

	@JvmStatic fun main(args: Array<String>)
	{
		val appId = args[0];

		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Beginning Game Loop Test      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val androidHome: String
		val localProperties = File("../../local.properties")
		if (localProperties.exists()) {
			val properties = Properties()
			properties.load(FileInputStream(localProperties))

			val sdkDir = properties.getProperty("sdk.dir")
			if (sdkDir != null) {
				androidHome = sdkDir
			} else {
				androidHome = System.getenv()["ANDROID_HOME"]!!
			}
		} else {
			androidHome = System.getenv()["ANDROID_HOME"]!!
		}

		val androidPlatformTools = "$androidHome/platform-tools"
		println("ANDROID_HOME: $androidPlatformTools")

		"$androidPlatformTools/adb install engine/android/build/outputs/apk/debug/android-debug.apk".runCommand()
		"$androidPlatformTools/adb shell am start -W -a com.google.intent.action.TEST_LOOP -n $appId/com.lyeeedar.AndroidLauncher -S".runCommand()
		"$androidPlatformTools/adb logcat -c".runCommand()
		val pid = "$androidPlatformTools/adb shell pidof $appId".runCommand()
		if (pid.isBlank()) {
			"$androidPlatformTools/adb logcat -d".runCommand()
			throw RuntimeException("App instantly crashed!")
		}

		val completeLogs = StringBuilder()
		while ("$androidPlatformTools/adb shell pidof $appId".runCommand().isNotBlank())
		{
			readLogs(androidPlatformTools, pid, completeLogs)
			Thread.sleep(5000) // 5 seconds
		}
		readLogs(androidPlatformTools, pid, completeLogs)

		var crash = ""

		var inCrash = false
		for (line in completeLogs.lines())
		{
			if (line.startsWith("--------- beginning of crash") || line.contains("FATAL EXCEPTION: GLThread"))
			{
				inCrash = true
			}
			else if (inCrash)
			{
				if (line.contains(" E "))
				{
					crash += line.split(" E ")[1] + "\n"
				}
				else
				{
					break
				}
			}
		}

		if (crash.isNotBlank())
		{
			throw RuntimeException(crash)
		}

		println("")
		println("#####      Game Loop Test Complete      #######")
		println("-------------------------------------------------------------------------")
	}
}