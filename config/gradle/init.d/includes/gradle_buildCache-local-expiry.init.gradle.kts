// if (GradleVersion.version("6.0") <= GradleVersion.current().baseVersion)
// Gradle.beforeSettings { } was added in Gradle 6.0
// buildCache.local.removeUnusedEntriesAfterDays is deprecated in Gradle 8.8 -> moved to gradle_caches-expiry.init.gradle.kts
// buildCache.local.removeUnusedEntriesAfterDays is scheduled for removal in Gradle 9.0

gradle.beforeSettings {
	buildCache {
		local {
			removeUnusedEntriesAfterDays = 365
		}
	}
}
