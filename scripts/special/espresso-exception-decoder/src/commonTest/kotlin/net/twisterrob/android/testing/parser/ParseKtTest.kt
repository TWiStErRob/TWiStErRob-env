package net.twisterrob.android.testing.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ParseKtTest {

	@Test
	fun test() {
		val actual = assertFails {
			parse("invalid")
		}
		assertEquals("Cannot match invalid", actual.message)
	}
}
