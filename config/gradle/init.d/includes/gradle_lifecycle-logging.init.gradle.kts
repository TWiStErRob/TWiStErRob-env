println(
	"""
		Current dir: ${gradle.startParameter.currentDir}
		Project dir: ${gradle.startParameter.projectDir}
		Version: ${gradle.gradleVersion}
		Init script: ${gradle.startParameter.allInitScripts}
		User home: ${gradle.gradleUserHomeDir}
		Gradle home: ${gradle.gradleHomeDir}
	""".trimIndent()
)
//Project root: ${rootProject.rootDir}
//Project name: ${rootProject.name}
gradle.settingsEvaluated { // this: Settings
	println("settingsEvaluated ${this@settingsEvaluated}")
}
gradle.allprojects { // this: Project
	println("allprojects ${this@allprojects}")
}
gradle.rootProject { // this: Project
	println("rootProject ${this@rootProject}")
	rootProject.plugins.whenPluginAdded {
		println("rootProject.whenPluginAdded ${this@whenPluginAdded}")
	}
}
gradle.projectsLoaded { // this: Gradle
	println("projectsLoaded ${this@projectsLoaded}")
}
gradle.beforeProject { // this: Project
	println("beforeProject ${this@beforeProject}")
}
gradle.afterProject { // this: Project
	println("afterProject ${this@afterProject}")
}
gradle.projectsEvaluated { // this: Gradle
	println("projectsEvaluated ${this@projectsEvaluated}")
}

if (!gradle.startParameter.isConfigurationCacheRequested) {
	gradle.buildFinished { // this: BuildResult
		println("buildFinished ${this@buildFinished}")
	}
}

/**
 * Reflective polyfill for Gradle 7.6's [StartParameter.isConfigurationCacheRequested].
 */
val StartParameter.isConfigurationCacheRequested: Boolean
	get() {
		val groovy = org.gradle.kotlin.dsl.GroovyBuilderScope.of(this).metaClass
		return groovy.hasProperty(this, "isConfigurationCacheRequested") != null
				&& groovy.getProperty(this, "isConfigurationCacheRequested") as Boolean
	}
