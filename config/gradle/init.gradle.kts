import org.gradle.util.GradleVersion

// NB: any changes to this script require a new daemon (`gradlew --stop` or `gradlew --no-daemon <tasks>`)

// See https://stackoverflow.com/a/48087543/253468
gradle.rootProject {
	val rootProject = this@rootProject
	// listen for lifecycle events on the project's plugins
	rootProject.plugins.whenPluginAdded plugin@{
		// check if any Android plugin is being applied (not necessarily just 'com.android.application')
		// this plugin is actually exactly for this purpose: to get notified
		if (this@plugin::class.java.name == "com.android.build.gradle.api.AndroidBasePlugin") {
			logger.info("Turning off `build/android-profile/profile-*.(rawproto|json)` generation.")
			// execute the hack in the context of the buildscript, not in this initscript
			if ("""^[3-4]\..*$""".toRegex().matches(gradle.gradleVersion)) {
				groovy.lang.GroovyShell(this@plugin::class.java.classLoader).evaluate(
					"""
					    com.android.build.gradle.internal.profile.ProfilerInitializer.recordingBuildListener =
					        new com.android.build.gradle.internal.profile.RecordingBuildListener(
					            com.android.builder.profile.ProcessProfileWriter.get());
					""".trimIndent()
				)
			}
		}
	}
}

if (GradleVersion.version("6.2.2") < GradleVersion.current().baseVersion) {
	// Closure receiver: Settings, Gradle.beforeSettings { } was added in Gradle 6.0
	// GradlePropertiesController was added in 6.2.2 https://github.com/gradle/gradle/commit/6abcc3fa4a01f276100955f0761096d705237be6
	gradle.beforeSettings settings@{
		// If there's nothing set, this value would be used.
		val defaul = org.gradle.internal.logging.DefaultLoggingConfiguration().warningMode
		// `--warning-mode=?` on command line (value == default if not set).
		val start = startParameter.warningMode
		// `-Porg.gradle.warning.mode=?` on command line, or `org.gradle.warning.mode=?` in gradle.properties.
		val prop = (gradle as org.gradle.api.internal.GradleInternal)
			.services
			.get(org.gradle.initialization.GradlePropertiesController::class.java)
			.gradleProperties
			.find("org.gradle.warning.mode")
		// Last non-null of [default, prop, start] wins during Gradle initialization.
		val actual = DEPRECATED_FEATURE_HANDLER.warningMode
		// Overriding to All to get every warning in my face, unless the project is set up already.
		val override = org.gradle.api.logging.configuration.WarningMode.All
		// Note: there's no way to get the actual command line passed to the gradlew command, nor the Gradle Daemon.
		// The main() args are parsed and never stored. Checked in Gradle 7.4.2. The standard ways:
		//  * java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments
		//  * java.lang.ProcessHandle.current().info().commandLine()
		// don't work either, because the Gradle Daemon process gets it the commands through a connection, not the command line.

		// I only want to override the value if there's nothing explicitly set.
		if (actual == defaul && start == defaul && prop == null) {
			// This is not possible to detect, as the default is not null.
			// This is a best effort to detect, it'll fail and override anyway if user explicitly launches `gradlew --warning-mode=summary`.
			logger.lifecycle(
					"${this@settings} has no Warning Mode specified, " +
							"using a default fallback in init script: --warning-mode=${override.name.toLowerCase()}."
			)
			DEPRECATED_FEATURE_HANDLER.warningMode = override
			gradle.startParameter.warningMode = override
		}
	}
}

val DEPRECATED_FEATURE_HANDLER: org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler
	get() = org.gradle.internal.deprecation
		.DeprecationLogger::class.java
		.getDeclaredField("DEPRECATED_FEATURE_HANDLER")
		.apply { isAccessible = true }
		.get(null) as org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler

var org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler
	.warningMode: org.gradle.api.logging.configuration.WarningMode
	get() = org.gradle.internal.featurelifecycle
		.LoggingDeprecatedFeatureHandler::class.java
		.getDeclaredField("warningMode")
		.apply { isAccessible = true }
		.get(this) as org.gradle.api.logging.configuration.WarningMode
	set(value) = org.gradle.internal.featurelifecycle
		.LoggingDeprecatedFeatureHandler::class.java
		.getDeclaredField("warningMode")
		.apply { isAccessible = true }
		.set(this, value)

if (GradleVersion.version("6.0") < GradleVersion.current().baseVersion) {
	gradle.beforeSettings {
		buildCache {
			local {
				removeUnusedEntriesAfterDays = 365
			}
		}
	}
}

/* // comment this line with a // at the end to enable all this logging
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
		println("whenPluginAdded ${this@whenPluginAdded}")
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
//*/
