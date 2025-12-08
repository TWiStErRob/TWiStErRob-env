plugins {
	kotlin("multiplatform") version "2.2.21"
}

kotlin {
	js(IR) {
		browser {
			binaries.executable()
			commonWebpackConfig {
				cssSupport {
					enabled.set(true)
				}
			}
		}
	}
	sourceSets {
		val commonMain by getting {
			dependencies {
			}
		}
		val jsMain by getting {
			dependencies {
				implementation(kotlin("stdlib-js"))
			}
		}
	}
}

group = "net.twisterrob.android"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

tasks.named<ProcessResources>("jsProcessResources") {
	val example = file("src/jsMain/resources/NoMatchingView.txt")
	inputs
		.file(example)
		.withPropertyName("exampleContent")
		.withPathSensitivity(PathSensitivity.NONE)
	filesMatching("index.html") {
		expand("exampleContent" to example.readText())
	}
}
