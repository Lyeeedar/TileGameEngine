plugins {
    kotlin("jvm")
}

sourceSets {
	main {
		java.srcDirs("src/", "../../game/desktop/src/")
		resources.srcDirs("../../game/assets")
	}
}

dependencies {
    val gdxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
	implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

tasks.register<Jar>("dist") {
    from(files(sourceSets.main.get().output.classesDirs))
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(file("../../game/assets"))

    manifest {
        attributes["Main-Class"] = "com.lyeeedar.desktop.DesktopLauncher"
    }
}

tasks.register<JavaExec>("run") {
	main = "com.lyeeedar.desktop.DesktopLauncher"
	classpath = sourceSets.main.get().runtimeClasspath
	standardInput = System.`in`
	workingDir = file("../../game/assets")
	isIgnoreExitValue = true

	if ("mac" in System.getProperty("os.name").toLowerCase()) {
		jvmArgs("-XstartOnFirstThread")
	}
}

project.apply {
	from("../../game/desktop/build.gradle.kts")
}