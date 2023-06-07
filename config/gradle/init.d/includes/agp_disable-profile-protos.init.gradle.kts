// See https://stackoverflow.com/a/48087543/253468
gradle.rootProject {
	val rootProject = this@rootProject
	// Listen for lifecycle events on the project's plugins.
	rootProject.plugins.whenPluginAdded plugin@{
		// Check if any Android plugin is being applied (not necessarily just 'com.android.application').
		// This plugin is actually exactly for this purpose: to get notified.
		if (this@plugin::class.java.name == "com.android.build.gradle.api.AndroidBasePlugin") {
			logger.info("Turning off `build/android-profile/profile-*.(rawproto|json)` generation.")
			// Execute the hack in the context of the buildscript, not in this initscript.
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
