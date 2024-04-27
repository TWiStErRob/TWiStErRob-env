// if (GradleVersion.version("8.0") <= GradleVersion.current().baseVersion)
// Settings.caches { } was added in Gradle 8.0
// Gradle.beforeSettings { } was added in Gradle 6.0
// caches.buildCache was addedin Gradle 8.8

// https://docs.gradle.org/8.0/userguide/directory_layout.html#dir:gradle_user_home:configure_cache_cleanup

gradle.beforeSettings {
	caches {
		// Uncomment to force cleanup and run `gradlew`.
		//cleanup = Cleanup.ALWAYS
		releasedWrappers { setRemoveUnusedEntriesAfterDays(3 * 365) }
		snapshotWrappers { setRemoveUnusedEntriesAfterDays(180) }
		downloadedResources { setRemoveUnusedEntriesAfterDays(365) }
		createdResources { setRemoveUnusedEntriesAfterDays(365) }
		if (GradleVersion.version("8.8") <= GradleVersion.current().baseVersion) {
			// Originally declared in gradle_buildCache-local-expiry.init.gradle.kts.
			buildCache { setRemoveUnusedEntriesAfterDays(365) }
		}
	}
}
