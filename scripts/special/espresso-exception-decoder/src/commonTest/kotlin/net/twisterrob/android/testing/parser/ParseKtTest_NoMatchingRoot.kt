@file:Suppress("detekt.MaxLineLength", "detekt.LongMethod")

package net.twisterrob.android.testing.parser

import net.twisterrob.android.testing.RootExceptionResult
import net.twisterrob.android.testing.renderer.RootNode
import net.twisterrob.android.testing.renderer.ViewNode
import net.twisterrob.android.testing.tests.loadTestResource
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseKtTest_NoMatchingRoot {

	@Test
	fun test() {
		val actual = parse(loadTestResource("NoMatchingRoot.txt"))

		val expected = RootExceptionResult(
			ex = "androidx.test.espresso.NoMatchingRootException",
			matcher = "is dialog",
			marker = "",
			rootText = $$"Root{application-window-token=android.view.ViewRootImpl$W@1becd3d7, window-token=android.view.ViewRootImpl$W@1becd3d7, has-window-focus=true, layout-params-type=1, layout-params-string=WM.LayoutParams{(0,0)(fillxfill) ty=1 fl=#81810100 pfl=0x8 wanim=0x1030461 surfaceInsets=Rect(0, 0 - 0, 0)}, decor-view-string=DecorView{id=-1, visibility=VISIBLE, width=1920, height=1080, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, layout-params=WM.LayoutParams{(0,0)(fillxfill) ty=1 fl=#81810100 pfl=0x8 wanim=0x1030461 surfaceInsets=Rect(0, 0 - 0, 0)}, tag=null, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}}",
			roots = mutableListOf(
				RootNode(
					name = $$"android.view.ViewRootImpl$W@1becd3d7",
					children = mutableListOf(
						ViewNode(
							level = 0,
							name = "DecorView",
							id = "-1",
							matches = false,
							children = mutableListOf(),
							props = mutableMapOf(
								"visibility" to "VISIBLE",
								"width" to "1920",
								"height" to "1080",
								"has-focus" to "false",
								"has-focusable" to "true",
								"has-window-focus" to "true",
								"is-clickable" to "false",
								"is-enabled" to "true",
								"is-focused" to "false",
								"is-focusable" to "false",
								"is-layout-requested" to "false",
								"is-selected" to "false",
								// TODO this is wrong.
								"layout-params" to "WM.LayoutParams{(0,0)(fillxfill)",
								"ty" to "1",
								"fl" to "#81810100",
								"pfl" to "0x8",
								"wanim" to "0x1030461",
								"surfaceInsets" to "Rect(0, 0 - 0, 0)}",
								"tag" to "null",
								"root-is-layout-requested" to "false",
								"has-input-connection" to "false",
								"x" to "0.0",
								"y" to "0.0",
								"child-count" to "2",
							),
						).apply { parent = null },
					),
					props = mutableMapOf(
						"application-window-token" to $$"android.view.ViewRootImpl$W@1becd3d7",
						"window-token" to $$"android.view.ViewRootImpl$W@1becd3d7",
						"has-window-focus" to "true",
						"layout-params-type" to "1",
						"name" to "WM.LayoutParams",
						"x" to "0",
						"y" to "0",
						"width" to "fill",
						"height" to "fill",
						"ty" to "1",
						"fl" to "#81810100",
						"pfl" to "0x8",
						"wanim" to "0x1030461",
						"surfaceInsets" to "Rect(0, 0 - 0, 0)",
						"layout-params-class" to "WM.LayoutParams",
					),
				).apply { parent = null },
			),
		)

		assertEquals(expected, actual)
	}
}
