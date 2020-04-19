import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
	id("io.fabric")
	id("com.google.gms.google-services")
}

val keystorePropertiesFile = rootProject.file("../PrivateStuff/keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

val appVersionCode: Int by project
val appVersion: String by project
val applicationId: String by project

android {
    buildToolsVersion("28.0.3")
    compileSdkVersion(28)
    sourceSets {
        named("main") {
	        manifest.srcFile("../../game/android/AndroidManifest.xml")
	        java.srcDirs("src/", "../../game/android/src/")
            res.srcDir("../../game/android/res")
            assets.srcDir("../../game/assets")
            jniLibs.srcDir("libs")
        }
    }
    defaultConfig {
        applicationId = applicationId
        minSdkVersion(16)
        targetSdkVersion(28)
        versionCode = appVersionCode
        versionName = appVersion
	    multiDexEnabled = true
    }

	signingConfigs {
		register("release") {
			keyAlias = keystoreProperties["keyAlias"] as String
			keyPassword = keystoreProperties["keyPassword"] as String
			storeFile = file(keystoreProperties["storeFile"] as String)
			storePassword = keystoreProperties["storePassword"] as String
		}
	}

	lintOptions {
		isAbortOnError = false
	}

    buildTypes {
        named("release") {
            isMinifyEnabled = true
	        isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
	        signingConfig = signingConfigs.getByName("release")
	        isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }
}

val natives: Configuration by configurations.creating

repositories {
	maven("https://maven.fabric.io/public")
}

dependencies {
    val gdxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
	implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
	natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi")
	natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
	natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
	natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
	natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")

	implementation("com.crashlytics.sdk.android:crashlytics:2.10.1@aar") { isTransitive = true }

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.3")

	// Add the Firebase SDK for Google Analytics
	implementation("com.google.firebase:firebase-analytics:17.2.2")
	implementation("com.google.firebase:firebase-perf:19.0.5")
}

// Called every time gradle gets executed, takes the native dependencies of
// the natives configuration, and extracts them to the proper libs/ folders
// so they get packed with the APK.
tasks.register("copyAndroidNatives") {
    doFirst {
        natives.files.forEach { jar ->
            val outputDir = file("libs/" + jar.nameWithoutExtension.substringAfterLast("natives-"))
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}
tasks.whenTaskAdded {
    if ("package" in name) {
        dependsOn("copyAndroidNatives")
    } else if ("processDebugGoogleServices" in name) {
	    dependsOn("copyGoogleServices")
    }
}

tasks.register<Exec>("run") {
	var path: String
	val localProperties = project.file("../../local.properties")
	if (localProperties.exists()) {
		val properties = Properties()
		properties.load(FileInputStream(localProperties))

		val sdkDir = properties.getProperty("sdk.dir")
		if (sdkDir != null) {
			path = sdkDir
		} else {
			path = System.getenv().get("ANDROID_HOME")!!
		}
	} else {
		path = System.getenv().get("ANDROID_HOME")!!
	}

	val adb = "$path/platform-tools/adb"
	commandLine(listOf("$adb", "shell am start -n com.lyeeedar/com.lyeeedar.AndroidLauncher"))
}

tasks.register("copyGoogleServices") {
	doFirst {
		val srcFile = project.file("../../game/android/google-services.json")
		val dstFile = project.file("google-services.json")
		if (!dstFile.exists())
		{
			srcFile.copyTo(dstFile, true)
		}
	}
}