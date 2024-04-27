// if (GradleVersion.version("8.8") <= GradleVersion.current().baseVersion)
// Settings.caches { } was added in Gradle 8.0
// Gradle.beforeSettings { } was added in Gradle 6.0
// caches.buildCache was added in Gradle 8.8

// https://docs.gradle.org/8.0/userguide/directory_layout.html#dir:gradle_user_home:configure_cache_cleanup

gradle.beforeSettings {
	caches {
		// Originally declared in gradle_buildCache-local-expiry.init.gradle.kts.
		buildCache { setRemoveUnusedEntriesAfterDays(365) }
	}
}
