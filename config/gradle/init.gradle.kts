import org.gradle.util.GradleVersion

// NB: any changes to this script require a new daemon (`gradlew --stop` or `gradlew --no-daemon <tasks>`)

//apply(from = "init.d/includes/gradle_lifecycle-logging.init.gradle.kts")
apply(from = "init.d/includes/agp_disable-profile-protos.init.gradle.kts")

if (GradleVersion.version("6.0") <= GradleVersion.current().baseVersion
	&& GradleVersion.current().baseVersion < GradleVersion.version("8.8")) {
	apply(from = "init.d/includes/gradle_buildCache-local-expiry.init.gradle.kts")
}

if (GradleVersion.version("6.2.2") <= GradleVersion.current().baseVersion) {
	apply(from = "init.d/includes/gradle_auto-warning.init.gradle.kts")
}

if (GradleVersion.version("8.0") <= GradleVersion.current().baseVersion) {
	apply(from = "init.d/includes/gradle_caches-expiry.80.init.gradle.kts")
}
if (GradleVersion.version("8.8") <= GradleVersion.current().baseVersion) {
	apply(from = "init.d/includes/gradle_caches-expiry.88.init.gradle.kts")
}
