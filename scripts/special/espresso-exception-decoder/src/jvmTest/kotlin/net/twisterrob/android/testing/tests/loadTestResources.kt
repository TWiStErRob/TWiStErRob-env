package net.twisterrob.android.testing.tests

private object Locator

actual fun loadTestResource(resourceName: String): String {
	val inputStream = Locator::class.java.classLoader?.getResourceAsStream(resourceName)
		?: error("Cannot find ${resourceName}")
	return inputStream.bufferedReader().use { it.readText() }
}
