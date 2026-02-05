import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

plugins {
	kotlin("multiplatform") version "2.3.10"
}

group = "net.twisterrob.android"
version = "1.0-SNAPSHOT"

kotlin {
	js(IR) {
		browser {
			binaries.executable()
			testTask {
				enabled = false
			}
			commonWebpackConfig {
				cssSupport {
					enabled = true
				}
			}
		}
	}
	jvm()
	applyDefaultHierarchyTemplate()
	@Suppress("unused")
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib"))
			}
		}
		val commonTest by getting {
			dependencies {
				implementation(kotlin("test"))
			}
		}
	}
}

plugins.withType<YarnPlugin> {
	extensions.getByType<YarnRootEnvSpec>().apply {
		yarnLockMismatchReport = YarnLockMismatchReport.WARNING
		yarnLockAutoReplace = true
	}
}

tasks.named<ProcessResources>("jsProcessResources") {
	val example = file("src/commonTest/resources/AmbiguousViewMatcher.txt")
	inputs
		.file(example)
		.withPropertyName("exampleContent")
		.withPathSensitivity(PathSensitivity.NONE)
	filesMatching("index.html") {
		expand("exampleContent" to example.readText())
	}
}
