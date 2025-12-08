package net.twisterrob.android.testing.parser

import net.twisterrob.android.testing.tests.loadTestResource
import kotlin.test.Test
import kotlin.test.assertFails

class ParseKtTest {

	@Test
	fun test() {
		assertFails {
			parse("")
		}
	}

	@Test
	fun testNoMatchingData() {
		parse(loadTestResource("NoMatchingData.txt"))
	}

	@Test
	fun testNoMatchingView() {
		parse(loadTestResource("NoMatchingView.txt"))
	}

	@Test
	fun testNoMatchingRoot() {
		parse(loadTestResource("NoMatchingRoot.txt"))
	}

	@Test
	fun testAmbiguousViewMatcher() {
		parse(loadTestResource("AmbiguousViewMatcher.txt"))
	}
}
