package com.lyeeedar.headless

import java.io.File
import java.io.FileInputStream
import java.util.*

object GameLoopTest
{
	@JvmStatic fun String.runCommand(print: Boolean = true): String {
		if (print) println(this)
		val output = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
		if (print) println(output)
		return output
	}

	fun readLogs(androidHome: String, pid: String, completeLog: StringBuilder)
	{
		val logs = "$androidHome/adb logcat -d".runCommand(false).split('\n')
		for (log in logs)
		{
			completeLog.append(log)
			if (log.contains(" $pid ")) println(log)
		}

		"$androidHome/adb logcat -c".runCommand(false)
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

		val apkPath = "../../engine/android/build/outputs/apk/debug/android-debug.apk"
		val apkFile = File(apkPath)
		if (!apkFile.exists()) {
			throw RuntimeException("Apk does not exist at " + apkFile.canonicalPath)
		}

		"$androidPlatformTools/adb install ${apkFile.canonicalPath}".runCommand()
		"$androidPlatformTools/adb shell am start -a com.google.intent.action.TEST_LOOP -n $appId/com.lyeeedar.AndroidLauncher -S".runCommand()
		"$androidPlatformTools/adb logcat -c".runCommand()

		var pidFailedCount = 0
		var pid = ""
		while (pid.isBlank())
		{
			pid = "$androidPlatformTools/adb shell pidof $appId".runCommand()
			Thread.sleep(1000) // 1 seconds

			pidFailedCount++
			if (pidFailedCount > 60*5) { // 5 min timeout
				val crashLogs = "$androidPlatformTools/adb logcat -d".runCommand().split('\n').filter { it.contains(" E ") || it.contains("lyeeedar") }.joinToString("\n")
				throw RuntimeException("##########################\nApp failed to start!\n##############################\n\n$crashLogs")
			}
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
			if (line.startsWith("--------- beginning of crash") || line.contains("FATAL EXCEPTION:"))
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