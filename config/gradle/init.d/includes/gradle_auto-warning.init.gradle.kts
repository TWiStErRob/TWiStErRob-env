import org.gradle.internal.logging.DefaultLoggingConfiguration
// if (GradleVersion.version("6.2.2") <= GradleVersion.current().baseVersion)
// Gradle.beforeSettings { } was added in Gradle 6.0
// GradlePropertiesController was added in 6.2.2 https://github.com/gradle/gradle/commit/6abcc3fa4a01f276100955f0761096d705237be6

gradle.beforeSettings settings@{ // this: Settings
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
	// don't work either,
	// because the Gradle Daemon process gets it the commands through a connection, not the command line.

	// I only want to override the value if there's nothing explicitly set.
	if (actual == defaul && start == defaul && prop == null) {
		// This is not possible to detect, as the default is not null, using a best effort implementation:
		// it'll fail and override anyway if user explicitly launches `gradlew --warning-mode=summary`.
		// Deprecated in Kotlin 1.5, starting from Gradle 8.0; for compatibility older, keeping the old method call.
		@Suppress("DEPRECATION")
		val overrideName = override.name.toLowerCase()
		logger.lifecycle(
			"${this@settings} has no Warning Mode specified, " +
					"using a default fallback in init script: --warning-mode=$overrideName, adjust gradle.properties:\n\torg.gradle.warning.mode=fail\n\torg.gradle.jvmargs=-Dorg.gradle.deprecation.trace=true"
		)
		DEPRECATED_FEATURE_HANDLER.warningMode = override
		gradle.startParameter.warningMode = override
	}
}

@Suppress("VariableNaming")
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
