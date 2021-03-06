plugins {
    kotlin("jvm")
	id("com.lyeeedar.gradle-plugins.rewriteSources")
}

dependencies {
    val gdxVersion: String by project
    val ktxVersion: String by project
	val squidlibVersion: String by project
	val kryoVersion: String by project
	val kotlinCoroutinesVersion: String by project

    implementation(kotlin("stdlib"))

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
	implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")

	implementation("com.esotericsoftware:kryo:$kryoVersion")

    implementation("io.github.libktx:ktx-actors:$ktxVersion")
    implementation("io.github.libktx:ktx-collections:$ktxVersion")
    implementation("io.github.libktx:ktx-math:$ktxVersion")
    implementation("io.github.libktx:ktx-scene2d:$ktxVersion")

	implementation("com.squidpony:squidlib-util:$squidlibVersion")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

	testImplementation("junit:junit:4.13")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

sourceSets {
	main {
		java.srcDirs("src/", "../../game/core/src/")
	}
	test {
		java.srcDirs("test/", "../../game/core/test/")
	}
}

tasks.rewriteSources {
    inputDirs = sourceSets.main.get().java.srcDirs
    srcDirs = sourceSets.main.get().java.srcDirs
}
tasks.compileKotlin {
	dependsOn(tasks.rewriteSources)
}

tasks.test {
	useJUnit()

	maxHeapSize = "1G"

	testLogging.showStandardStreams = true
	testLogging.setEvents(listOf("standardOut", "passed", "skipped", "failed"))
}

project.apply {
	from("../../game/core/build.gradle.kts")
}