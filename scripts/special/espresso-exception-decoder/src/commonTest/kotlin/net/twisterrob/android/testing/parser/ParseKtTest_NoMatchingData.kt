@file:Suppress("detekt.MaxLineLength", "detekt.LongMethod", "detekt.LargeClass", "detekt.ClassNaming")

package net.twisterrob.android.testing.parser

import net.twisterrob.android.testing.DataExceptionResult
import net.twisterrob.android.testing.renderer.DataNode
import net.twisterrob.android.testing.tests.loadTestResource
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseKtTest_NoMatchingData {

	@Suppress("detekt.CyclomaticComplexMethod")
	@Test
	fun test() {
		val actual = parse(loadTestResource("NoMatchingData.txt"))

		val expected = DataExceptionResult(
			ex = "java.lang.RuntimeException",
			matcher = "with string from resource id: <2131165435>",
			dataText = "Data: Preferences (class: android.preference.PreferenceCategory) token: 0, " +
					"Data: Default Details Page Automatic (class: android.preference.ListPreference) token: 1, " +
					"Data: Use internal image viewer Internal image viewer has limited functionality, but it definitely shows the images, while using another application may result in crashes out of this app's control. (class: android.preference.CheckBoxPreference) token: 2, " +
					"Data: Suggest Categories When better (class: android.preference.ListPreference) token: 3, " +
					"Data: Links (class: android.preference.PreferenceCategory) token: 4, " +
					"Data: App info in Settings (class: android.preference.Preference) token: 5, " +
					"Data: App details in Play Store (class: android.preference.Preference) token: 6, " +
					"Data: Feedback Ask a question, request features you'd like to see, or just tell me what you think about the app. Any feedback is welcome. (class: android.preference.Preference) token: 7, " +
					"Data: About !Magic Home Inventory (class: android.preference.Preference) token: 8, " +
					"Data: Advanced (class: android.preference.PreferenceCategory) token: 9, " +
					"Data: Sunburst Ignore Level Sunburst diagram can be slow if there are a lot of items shown. Decrease this setting to ignore items stored deep within other items from displaying. Values below 3 are not advised. (class: net.twisterrob.android.view.NumberPickerPreference) token: 10, " +
					"Data: Display more details (debug) Display some internal details of the entities, these information are only relevant for debugging and hacking. (class: android.preference.CheckBoxPreference) token: 11, " +
					"Data: Highlight Matches Display the edit distance of the entered name and the match in the suggestions list. (class: android.preference.CheckBoxPreference) token: 12, " +
					"Data: Manage Inventory Space (class: android.preference.Preference) token: 13",
			data = mutableListOf(
				DataNode(
					name = "android.preference.PreferenceCategory",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "0",
						"class" to "android.preference.PreferenceCategory",
						"data" to "Preferences",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.ListPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "1",
						"class" to "android.preference.ListPreference",
						"data" to "Default Details Page Automatic",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.CheckBoxPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "2",
						"class" to "android.preference.CheckBoxPreference",
						"data" to "Use internal image viewer Internal image viewer has limited functionality, but it definitely shows the images, while using another application may result in crashes out of this app's control.",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.ListPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "3",
						"class" to "android.preference.ListPreference",
						"data" to "Suggest Categories When better",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.PreferenceCategory",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "4",
						"class" to "android.preference.PreferenceCategory",
						"data" to "Links",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.Preference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "5",
						"class" to "android.preference.Preference",
						"data" to "App info in Settings",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.Preference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "6",
						"class" to "android.preference.Preference",
						"data" to "App details in Play Store",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.Preference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "7",
						"class" to "android.preference.Preference",
						"data" to "Feedback Ask a question, request features you'd like to see, or just tell me what you think about the app. Any feedback is welcome.",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.Preference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "8",
						"class" to "android.preference.Preference",
						"data" to "About !Magic Home Inventory",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.PreferenceCategory",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "9",
						"class" to "android.preference.PreferenceCategory",
						"data" to "Advanced",
					),
				).apply { parent = null },
				DataNode(
					name = "net.twisterrob.android.view.NumberPickerPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "10",
						"class" to "net.twisterrob.android.view.NumberPickerPreference",
						"data" to "Sunburst Ignore Level Sunburst diagram can be slow if there are a lot of items shown. Decrease this setting to ignore items stored deep within other items from displaying. Values below 3 are not advised.",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.CheckBoxPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "11",
						"class" to "android.preference.CheckBoxPreference",
						"data" to "Display more details (debug) Display some internal details of the entities, these information are only relevant for debugging and hacking.",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.CheckBoxPreference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "12",
						"class" to "android.preference.CheckBoxPreference",
						"data" to "Highlight Matches Display the edit distance of the entered name and the match in the suggestions list.",
					),
				).apply { parent = null },
				DataNode(
					name = "android.preference.Preference",
					children = mutableListOf(),
					props = mutableMapOf(
						"token" to "13",
						"class" to "android.preference.Preference",
						"data" to "Manage Inventory Space",
					),
				).apply { parent = null },
			),
		)

		assertEquals(expected, actual)
	}
}
