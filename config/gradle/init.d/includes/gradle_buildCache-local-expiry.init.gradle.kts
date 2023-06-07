// if (GradleVersion.version("6.0") <= GradleVersion.current().baseVersion)
// Gradle.beforeSettings { } was added in Gradle 6.0

gradle.beforeSettings {
	buildCache {
		local {
			removeUnusedEntriesAfterDays = 365
		}
	}
}
