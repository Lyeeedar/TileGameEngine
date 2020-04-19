plugins {
    kotlin("jvm")
}

sourceSets {
	main {
		java.srcDirs("src/", "../../game/headless/src/")
		resources.srcDirs("../../game/assets")
	}
}

dependencies {
    val gdxVersion: String by project
	val ktxVersion: String by project

    implementation(project(":core"))

    implementation(kotlin("stdlib"))

	implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
	implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
	implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
	implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")

	implementation("io.github.libktx:ktx-collections:$ktxVersion")

	implementation("org.mockito:mockito-all:1.10.19")

	implementation("com.auth0:java-jwt:3.9.0")
	implementation("com.google.api-client:google-api-client:1.30.7")
	implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20191113-1.30.3")

	implementation("com.google.cloud:google-cloud-translate:1.94.2")
	implementation("org.languagetool:language-en:4.8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

tasks.register<JavaExec>("compileResources") {
	main = "com.lyeeedar.headless.CompilerRunner"
	classpath = sourceSets.main.get().runtimeClasspath
	standardInput = System.`in`
	workingDir = file("../../game/assets")
}

tasks.register<JavaExec>("testResources") {
	main = "com.lyeeedar.headless.AssetTester"
	classpath = sourceSets.main.get().runtimeClasspath
	standardInput = System.`in`
	workingDir = file("../../game/assets")
}

tasks.register<JavaExec>("releaseAndroidToPlaystore") {
	main = "com.lyeeedar.headless.AndroidRelease"
	classpath = sourceSets.main.get().runtimeClasspath
	standardInput = System.`in`
	workingDir = file("../../game/assets")
}

tasks.register<JavaExec>("autoLocalise") {
	main = "com.lyeeedar.headless.AutoLocaliser"
	classpath = sourceSets.main.get().runtimeClasspath
	standardInput = System.`in`
	workingDir = file("../../game/assets")
}

project.apply {
	from("../../game/headless/build.gradle.kts")
}