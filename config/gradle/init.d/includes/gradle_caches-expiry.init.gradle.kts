// if (GradleVersion.version("8.0") <= GradleVersion.current().baseVersion)
// Settings.caches { } was added in Gradle 8.0
// Gradle.beforeSettings { } was added in Gradle 6.0

// https://docs.gradle.org/8.0/userguide/directory_layout.html#dir:gradle_user_home:configure_cache_cleanup

gradle.beforeSettings {
	caches {
		releasedWrapper { setRemoveUnusedEntriesAfterDays(3 * 365) }
		snapshotWrappers { setRemoveUnusedEntriesAfterDays(180) }
		downloadedResources { setRemoveUnusedEntriesAfterDays(365) }
		createdResources { setRemoveUnusedEntriesAfterDays(365) }
	}
}
