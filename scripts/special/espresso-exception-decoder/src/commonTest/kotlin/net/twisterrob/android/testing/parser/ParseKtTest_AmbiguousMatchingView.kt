@file:Suppress("detekt.MaxLineLength", "detekt.LongMethod")

package net.twisterrob.android.testing.parser

import net.twisterrob.android.testing.ViewExceptionResult
import net.twisterrob.android.testing.renderer.ViewNode
import net.twisterrob.android.testing.tests.loadTestResource
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseKtTest_AmbiguousMatchingView {

	@Test
	fun test() {
		val actual = parse(loadTestResource("AmbiguousViewMatcher.txt"))

		val stack = $$"""
			at dalvik.system.VMStack.getThreadStackTrace(Native Method)
			at java.lang.Thread.getStackTrace(Thread.java:580)
			at android.support.test.espresso.base.DefaultFailureHandler.getUserFriendlyError(DefaultFailureHandler.java:92)
			at android.support.test.espresso.base.DefaultFailureHandler.handle(DefaultFailureHandler.java:56)
			at net.twisterrob.android.test.junit.AndroidJUnitRunner$DetailedFailureHandler.handle(AndroidJUnitRunner.java:81)
			at android.support.test.espresso.ViewInteraction.runSynchronouslyOnUiThread(ViewInteraction.java:184)
			at android.support.test.espresso.ViewInteraction.check(ViewInteraction.java:158)
			at net.twisterrob.inventory.android.test.actors.PropertyEditActivityActor.checkPicture(PropertyEditActivityActor.java:63)
			at net.twisterrob.inventory.android.activity.PropertyEditActivityTest_Create.checkEverythingFilledIn(PropertyEditActivityTest_Create.java:221)
			at net.twisterrob.inventory.android.activity.PropertyEditActivityTest_Create.testRotate(PropertyEditActivityTest_Create.java:102)
			at java.lang.reflect.Method.invoke(Native Method)
			at java.lang.reflect.Method.invoke(Method.java:372)
			at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
			at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
			at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
			at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
			at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
			at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
			at net.twisterrob.android.test.junit.SensibleActivityTestRule$TestLogger$1.evaluate(SensibleActivityTestRule.java:87)
			at android.support.test.internal.statement.UiThreadStatement.evaluate(UiThreadStatement.java:55)
			at android.support.test.rule.ActivityTestRule$ActivityStatement.evaluate(ActivityTestRule.java:270)
			at net.twisterrob.android.test.espresso.ScreenshotFailure$ScreenshotStatement.evaluate(ScreenshotFailure.java:69)
			at net.twisterrob.android.test.junit.IdlingResourceRule$IdlingResourceStatement.evaluate(IdlingResourceRule.java:33)
			at net.twisterrob.android.test.junit.IdlingResourceRule$IdlingResourceStatement.evaluate(IdlingResourceRule.java:33)
			at net.twisterrob.android.test.junit.IdlingResourceRule$IdlingResourceStatement.evaluate(IdlingResourceRule.java:33)
			at net.twisterrob.inventory.android.test.TestDatabaseRule$DatabaseStatement.evaluate(TestDatabaseRule.java:34)
			at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:55)
			at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:55)
			at org.junit.rules.ExternalResource$1.evaluate(ExternalResource.java:48)
			at org.junit.rules.RunRules.evaluate(RunRules.java:20)
			at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
			at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
			at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
			at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
			at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
			at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
			at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
			at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
			at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
			at org.junit.runners.Suite.runChild(Suite.java:128)
			at org.junit.runners.Suite.runChild(Suite.java:27)
			at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
			at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
			at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
			at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
			at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
			at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
			at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
			at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
			at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
			at android.support.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:262)
			at net.twisterrob.android.test.junit.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:31)
			at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1837)
			
		""".trimIndent()
		val hierarchy = """
			+>DecorView{id=-1, visibility=VISIBLE, width=1794, height=1080, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}
			|
			+->LinearLayout{id=-1, visibility=VISIBLE, width=1794, height=1080, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}
			|
			+-->ViewStub{id=16909166, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}
			|
			+-->FrameLayout{id=-1, visibility=VISIBLE, width=1794, height=1005, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=75.0, child-count=1}
			|
			+--->ActionBarOverlayLayout{id=2131624054, res-name=decor_content_parent, visibility=VISIBLE, width=1794, height=1005, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}
			|
			+---->ContentFrameLayout{id=16908290, res-name=content, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=144.0, child-count=1}
			|
			+----->FrameLayout{id=-1, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}
			|
			+------>FrameLayout{id=2131623940, res-name=activityRoot, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}
			|
			+------->LinearLayout{id=2131623946, res-name=fragmentRoot, visibility=VISIBLE, width=1770, height=837, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=12.0, y=12.0, child-count=2}
			|
			+-------->ScrollView{id=-1, visibility=VISIBLE, width=1770, height=693, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}
			|
			+--------->LinearLayout{id=-1, visibility=VISIBLE, width=1770, height=983, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=5}
			|
			+---------->FrameLayout{id=-1, visibility=VISIBLE, width=1770, height=384, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}
			|
			+----------->AppCompatImageView{id=2131623948, res-name=image, visibility=VISIBLE, width=1770, height=384, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0} ****MATCHES****
			|
			+----------->AppCompatImageView{id=2131623960, res-name=type, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}
			|
			+---------->LinearLayout{id=-1, visibility=VISIBLE, width=1770, height=72, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=384.0, child-count=2}
			|
			+----------->AppCompatSpinner{id=2131624103, res-name=type_edit, visibility=VISIBLE, width=1770, height=72, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}
			|
			+------------>RelativeLayout{id=2131623941, res-name=adapterRoot, visibility=VISIBLE, width=1722, height=72, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=4}
			|
			+------------->RigidImageView{id=2131623948, res-name=image, visibility=VISIBLE, width=48, height=48, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=12.0, y=12.0} ****MATCHES****
			|
			+------------->Space{id=2131624034, res-name=spacer, visibility=INVISIBLE, width=48, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=72.0, y=0.0}
			|
			+------------->AppCompatTextView{id=2131623960, res-name=type, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, text=, input-type=0, ime-target=false, has-links=false}
			|
			+------------->AppCompatTextView{id=2131623958, res-name=title, visibility=VISIBLE, width=1044, height=57, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=120.0, y=0.0, text=House (townhouse, cottage, bungalow, suburban house), input-type=0, ime-target=false, has-links=false}
			|
			+----------->AppCompatImageButton{id=2131624104, res-name=help, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}
			|
			+---------->RecyclerView{id=16908293, res-name=hint, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=0}
			|
			+---------->TextInputLayout{id=-1, visibility=VISIBLE, width=1770, height=169, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=456.0, child-count=1}
			|
			+----------->TextInputEditText{id=2131623958, res-name=title, visibility=VISIBLE, width=1770, height=136, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=true, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=true, editor-info=[inputType=0x2001 imeOptions=0x8000005 privateImeOptions=null actionLabel=null actionId=0 initialSelStart=0 initialSelEnd=0 initialCapsMode=0x2000 hintText=Property Title label=null packageName=null fieldId=0 fieldName=null extras=null ], x=0.0, y=33.0, text=Test Property, input-type=8193, ime-target=true, has-links=false}
			|
			+---------->TextInputLayout{id=-1, visibility=VISIBLE, width=1770, height=358, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=625.0, child-count=1}
			|
			+----------->TextInputEditText{id=2131623944, res-name=description, visibility=VISIBLE, width=1770, height=325, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=true, editor-info=[inputType=0x20001 imeOptions=0x44000006 privateImeOptions=null actionLabel=null actionId=0 initialSelStart=0 initialSelEnd=0 initialCapsMode=0x0 hintText=Property Description label=null packageName=null fieldId=0 fieldName=null extras=null ], x=0.0, y=33.0, text=Test Description
			ÁÉÓÖŐÚÜŰ
			ماهو الاسم؟, input-type=131073, ime-target=false, has-links=false}
			|
			+-------->AppCompatButton{id=2131623942, res-name=btn_save, visibility=VISIBLE, width=264, height=144, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=753.0, y=693.0, text=Save, input-type=0, ime-target=false, has-links=false}
			|
			+---->ActionBarContainer{id=2131624055, res-name=action_bar_container, visibility=VISIBLE, width=1794, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}
			|
			+----->Toolbar{id=2131624056, res-name=action_bar, visibility=VISIBLE, width=1794, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=3}
			|
			+------>ImageButton{id=-1, desc=Navigate up, visibility=VISIBLE, width=168, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}
			|
			+------>TextView{id=-1, visibility=VISIBLE, width=256, height=57, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=216.0, y=43.0, text=New Property, input-type=0, ime-target=false, has-links=false}
			|
			+------>ActionMenuView{id=-1, visibility=VISIBLE, width=276, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=1518.0, y=0.0, child-count=2}
			|
			+------->ActionMenuItemView{id=2131624205, res-name=action_picture_get, desc=Take Picture, visibility=VISIBLE, width=168, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, text=, input-type=0, ime-target=false, has-links=false}
			|
			+------->OverflowMenuButton{id=-1, desc=More options, visibility=VISIBLE, width=108, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=168.0, y=0.0}
			|
			+----->ActionBarContextView{id=2131624057, res-name=action_context_bar, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=0}
			|
		""".trimIndent()
		val expected = ViewExceptionResult(
			ex = "android.support.test.espresso.AmbiguousViewMatcherException",
			matcher = "with id: net.twisterrob.inventory.debug:id/image",
			marker = "****MATCHES****",
			hierarchyText = hierarchy + "\n" + stack,
			hierarchyArray = listOf(
				"+>DecorView{id=-1, visibility=VISIBLE, width=1794, height=1080, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}",
				"+->LinearLayout{id=-1, visibility=VISIBLE, width=1794, height=1080, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}",
				"+-->ViewStub{id=16909166, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}",
				"+-->FrameLayout{id=-1, visibility=VISIBLE, width=1794, height=1005, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=75.0, child-count=1}",
				"+--->ActionBarOverlayLayout{id=2131624054, res-name=decor_content_parent, visibility=VISIBLE, width=1794, height=1005, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}",
				"+---->ContentFrameLayout{id=16908290, res-name=content, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=144.0, child-count=1}",
				"+----->FrameLayout{id=-1, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}",
				"+------>FrameLayout{id=2131623940, res-name=activityRoot, visibility=VISIBLE, width=1794, height=861, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}",
				"+------->LinearLayout{id=2131623946, res-name=fragmentRoot, visibility=VISIBLE, width=1770, height=837, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=12.0, y=12.0, child-count=2}",
				"+-------->ScrollView{id=-1, visibility=VISIBLE, width=1770, height=693, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}",
				"+--------->LinearLayout{id=-1, visibility=VISIBLE, width=1770, height=983, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=5}",
				"+---------->FrameLayout{id=-1, visibility=VISIBLE, width=1770, height=384, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}",
				"+----------->AppCompatImageView{id=2131623948, res-name=image, visibility=VISIBLE, width=1770, height=384, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0} ****MATCHES****",
				"+----------->AppCompatImageView{id=2131623960, res-name=type, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}",
				"+---------->LinearLayout{id=-1, visibility=VISIBLE, width=1770, height=72, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=384.0, child-count=2}",
				"+----------->AppCompatSpinner{id=2131624103, res-name=type_edit, visibility=VISIBLE, width=1770, height=72, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=1}",
				"+------------>RelativeLayout{id=2131623941, res-name=adapterRoot, visibility=VISIBLE, width=1722, height=72, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=4}",
				"+------------->RigidImageView{id=2131623948, res-name=image, visibility=VISIBLE, width=48, height=48, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=12.0, y=12.0} ****MATCHES****",
				"+------------->Space{id=2131624034, res-name=spacer, visibility=INVISIBLE, width=48, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=72.0, y=0.0}",
				"+------------->AppCompatTextView{id=2131623960, res-name=type, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, text=, input-type=0, ime-target=false, has-links=false}",
				"+------------->AppCompatTextView{id=2131623958, res-name=title, visibility=VISIBLE, width=1044, height=57, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=120.0, y=0.0, text=House (townhouse, cottage, bungalow, suburban house), input-type=0, ime-target=false, has-links=false}",
				"+----------->AppCompatImageButton{id=2131624104, res-name=help, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}",
				"+---------->RecyclerView{id=16908293, res-name=hint, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=0}",
				"+---------->TextInputLayout{id=-1, visibility=VISIBLE, width=1770, height=169, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=456.0, child-count=1}",
				"+----------->TextInputEditText{id=2131623958, res-name=title, visibility=VISIBLE, width=1770, height=136, has-focus=true, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=true, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=true, editor-info=[inputType=0x2001 imeOptions=0x8000005 privateImeOptions=null actionLabel=null actionId=0 initialSelStart=0 initialSelEnd=0 initialCapsMode=0x2000 hintText=Property Title label=null packageName=null fieldId=0 fieldName=null extras=null ], x=0.0, y=33.0, text=Test Property, input-type=8193, ime-target=true, has-links=false}",
				"+---------->TextInputLayout{id=-1, visibility=VISIBLE, width=1770, height=358, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=625.0, child-count=1}",
				"+----------->TextInputEditText{id=2131623944, res-name=description, visibility=VISIBLE, width=1770, height=325, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=true, editor-info=[inputType=0x20001 imeOptions=0x44000006 privateImeOptions=null actionLabel=null actionId=0 initialSelStart=0 initialSelEnd=0 initialCapsMode=0x0 hintText=Property Description label=null packageName=null fieldId=0 fieldName=null extras=null ], x=0.0, y=33.0, text=Test Description\nÁÉÓÖŐÚÜŰ\nماهو الاسم؟, input-type=131073, ime-target=false, has-links=false}",
				"+-------->AppCompatButton{id=2131623942, res-name=btn_save, visibility=VISIBLE, width=264, height=144, has-focus=false, has-focusable=true, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=753.0, y=693.0, text=Save, input-type=0, ime-target=false, has-links=false}",
				"+---->ActionBarContainer{id=2131624055, res-name=action_bar_container, visibility=VISIBLE, width=1794, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=2}",
				"+----->Toolbar{id=2131624056, res-name=action_bar, visibility=VISIBLE, width=1794, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=3}",
				"+------>ImageButton{id=-1, desc=Navigate up, visibility=VISIBLE, width=168, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0}",
				"+------>TextView{id=-1, visibility=VISIBLE, width=256, height=57, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=216.0, y=43.0, text=New Property, input-type=0, ime-target=false, has-links=false}",
				"+------>ActionMenuView{id=-1, visibility=VISIBLE, width=276, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=1518.0, y=0.0, child-count=2}",
				"+------->ActionMenuItemView{id=2131624205, res-name=action_picture_get, desc=Take Picture, visibility=VISIBLE, width=168, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, text=, input-type=0, ime-target=false, has-links=false}",
				"+------->OverflowMenuButton{id=-1, desc=More options, visibility=VISIBLE, width=108, height=144, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=true, is-enabled=true, is-focused=false, is-focusable=true, is-layout-requested=false, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=168.0, y=0.0}",
				"+----->ActionBarContextView{id=2131624057, res-name=action_context_bar, visibility=GONE, width=0, height=0, has-focus=false, has-focusable=false, has-window-focus=true, is-clickable=false, is-enabled=true, is-focused=false, is-focusable=false, is-layout-requested=true, is-selected=false, root-is-layout-requested=false, has-input-connection=false, x=0.0, y=0.0, child-count=0}",
			),
			stacktrace = stack,
			resNames = listOf(
				"action_bar",
				"action_bar_container",
				"action_context_bar",
				"action_picture_get",
				"activityRoot",
				"adapterRoot",
				"btn_save",
				"content",
				"decor_content_parent",
				"description",
				"fragmentRoot",
				"help",
				"hint",
				"image",
				"spacer",
				"title",
				"type",
				"type_edit",
			),
			types = emptyList(),
			hierarchy = ViewNode(
				level = 0,
				name = "DecorView",
				id = "-1",
				matches = false,
				children = mutableListOf(
					ViewNode(
						level = 1,
						name = "LinearLayout",
						id = "-1",
						matches = false,
						children = mutableListOf(
							ViewNode(
								level = 2,
								name = "ViewStub",
								id = "16909166",
								matches = false,
								children = mutableListOf(),
								props = mutableMapOf(
									"visibility" to "GONE",
									"width" to "0",
									"height" to "0",
									"has-focus" to "false",
									"has-focusable" to "false",
									"has-window-focus" to "true",
									"is-clickable" to "false",
									"is-enabled" to "true",
									"is-focused" to "false",
									"is-focusable" to "false",
									"is-layout-requested" to "true",
									"is-selected" to "false",
									"root-is-layout-requested" to "false",
									"has-input-connection" to "false",
									"x" to "0.0",
									"y" to "0.0",
								),
							),
							ViewNode(
								level = 2,
								name = "FrameLayout",
								id = "-1",
								matches = false,
								children = mutableListOf(
									ViewNode(
										level = 3,
										name = "ActionBarOverlayLayout",
										id = "2131624054",
										matches = false,
										children = mutableListOf(
											ViewNode(
												level = 4,
												name = "ContentFrameLayout",
												id = "16908290",
												matches = false,
												children = mutableListOf(
													ViewNode(
														level = 5,
														name = "FrameLayout",
														id = "-1",
														matches = false,
														children = mutableListOf(
															ViewNode(
																level = 6,
																name = "FrameLayout",
																id = "2131623940",
																matches = false,
																children = mutableListOf(
																	ViewNode(
																		level = 7,
																		name = "LinearLayout",
																		id = "2131623946",
																		matches = false,
																		children = mutableListOf(
																			ViewNode(
																				level = 8,
																				name = "ScrollView",
																				id = "-1",
																				matches = false,
																				children = mutableListOf(
																					ViewNode(
																						level = 9,
																						name = "LinearLayout",
																						id = "-1",
																						matches = false,
																						children = mutableListOf(
																							ViewNode(
																								level = 10,
																								name = "FrameLayout",
																								id = "-1",
																								matches = false,
																								children = mutableListOf(
																									ViewNode(
																										level = 11,
																										name = "AppCompatImageView",
																										id = "2131623948",
																										matches = true,
																										children = mutableListOf(),
																										props = mutableMapOf(
																											"res-name" to "image",
																											"visibility" to "VISIBLE",
																											"width" to "1770",
																											"height" to "384",
																											"has-focus" to "false",
																											"has-focusable" to "false",
																											"has-window-focus" to "true",
																											"is-clickable" to "true",
																											"is-enabled" to "true",
																											"is-focused" to "false",
																											"is-focusable" to "false",
																											"is-layout-requested" to "false",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "false",
																											"x" to "0.0",
																											"y" to "0.0",
																										),
																									),
																									ViewNode(
																										level = 11,
																										name = "AppCompatImageView",
																										id = "2131623960",
																										matches = false,
																										children = mutableListOf(),
																										props = mutableMapOf(
																											"res-name" to "type",
																											"visibility" to "GONE",
																											"width" to "0",
																											"height" to "0",
																											"has-focus" to "false",
																											"has-focusable" to "false",
																											"has-window-focus" to "true",
																											"is-clickable" to "false",
																											"is-enabled" to "true",
																											"is-focused" to "false",
																											"is-focusable" to "false",
																											"is-layout-requested" to "true",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "false",
																											"x" to "0.0",
																											"y" to "0.0",
																										),
																									),
																								),
																								props = mutableMapOf(
																									"visibility" to "VISIBLE",
																									"width" to "1770",
																									"height" to "384",
																									"has-focus" to "false",
																									"has-focusable" to "false",
																									"has-window-focus" to "true",
																									"is-clickable" to "false",
																									"is-enabled" to "true",
																									"is-focused" to "false",
																									"is-focusable" to "false",
																									"is-layout-requested" to "false",
																									"is-selected" to "false",
																									"root-is-layout-requested" to "false",
																									"has-input-connection" to "false",
																									"x" to "0.0",
																									"y" to "0.0",
																									"child-count" to "2",
																								),
																							),
																							ViewNode(
																								level = 10,
																								name = "LinearLayout",
																								id = "-1",
																								matches = false,
																								children = mutableListOf(
																									ViewNode(
																										level = 11,
																										name = "AppCompatSpinner",
																										id = "2131624103",
																										matches = false,
																										children = mutableListOf(
																											ViewNode(
																												level = 12,
																												name = "RelativeLayout",
																												id = "2131623941",
																												matches = false,
																												children = mutableListOf(
																													ViewNode(
																														level = 13,
																														name = "RigidImageView",
																														id = "2131623948",
																														matches = true,
																														children = mutableListOf(),
																														props = mutableMapOf(
																															"res-name" to "image",
																															"visibility" to "VISIBLE",
																															"width" to "48",
																															"height" to "48",
																															"has-focus" to "false",
																															"has-focusable" to "false",
																															"has-window-focus" to "true",
																															"is-clickable" to "false",
																															"is-enabled" to "true",
																															"is-focused" to "false",
																															"is-focusable" to "false",
																															"is-layout-requested" to "false",
																															"is-selected" to "false",
																															"root-is-layout-requested" to "false",
																															"has-input-connection" to "false",
																															"x" to "12.0",
																															"y" to "12.0",
																														),
																													),
																													ViewNode(
																														level = 13,
																														name = "Space",
																														id = "2131624034",
																														matches = false,
																														children = mutableListOf(),
																														props = mutableMapOf(
																															"res-name" to "spacer",
																															"visibility" to "INVISIBLE",
																															"width" to "48",
																															"height" to "0",
																															"has-focus" to "false",
																															"has-focusable" to "false",
																															"has-window-focus" to "true",
																															"is-clickable" to "false",
																															"is-enabled" to "true",
																															"is-focused" to "false",
																															"is-focusable" to "false",
																															"is-layout-requested" to "false",
																															"is-selected" to "false",
																															"root-is-layout-requested" to "false",
																															"has-input-connection" to "false",
																															"x" to "72.0",
																															"y" to "0.0",
																														),
																													),
																													ViewNode(
																														level = 13,
																														name = "AppCompatTextView",
																														id = "2131623960",
																														matches = false,
																														children = mutableListOf(),
																														props = mutableMapOf(
																															"res-name" to "type",
																															"visibility" to "GONE",
																															"width" to "0",
																															"height" to "0",
																															"has-focus" to "false",
																															"has-focusable" to "false",
																															"has-window-focus" to "true",
																															"is-clickable" to "false",
																															"is-enabled" to "true",
																															"is-focused" to "false",
																															"is-focusable" to "false",
																															"is-layout-requested" to "true",
																															"is-selected" to "false",
																															"root-is-layout-requested" to "false",
																															"has-input-connection" to "false",
																															"x" to "0.0",
																															"y" to "0.0",
																															"text" to "",
																															"input-type" to "0",
																															"ime-target" to "false",
																															"has-links" to "false",
																														),
																													),
																													ViewNode(
																														level = 13,
																														name = "AppCompatTextView",
																														id = "2131623958",
																														matches = false,
																														children = mutableListOf(),
																														props = mutableMapOf(
																															"res-name" to "title",
																															"visibility" to "VISIBLE",
																															"width" to "1044",
																															"height" to "57",
																															"has-focus" to "false",
																															"has-focusable" to "false",
																															"has-window-focus" to "true",
																															"is-clickable" to "false",
																															"is-enabled" to "true",
																															"is-focused" to "false",
																															"is-focusable" to "false",
																															"is-layout-requested" to "false",
																															"is-selected" to "false",
																															"root-is-layout-requested" to "false",
																															"has-input-connection" to "false",
																															"x" to "120.0",
																															"y" to "0.0",
																															"text" to "House (townhouse, cottage, bungalow, suburban house)",
																															"input-type" to "0",
																															"ime-target" to "false",
																															"has-links" to "false",
																														),
																													),
																												),
																												props = mutableMapOf(
																													"res-name" to "adapterRoot",
																													"visibility" to "VISIBLE",
																													"width" to "1722",
																													"height" to "72",
																													"has-focus" to "false",
																													"has-focusable" to "false",
																													"has-window-focus" to "true",
																													"is-clickable" to "false",
																													"is-enabled" to "true",
																													"is-focused" to "false",
																													"is-focusable" to "false",
																													"is-layout-requested" to "false",
																													"is-selected" to "false",
																													"root-is-layout-requested" to "false",
																													"has-input-connection" to "false",
																													"x" to "0.0",
																													"y" to "0.0",
																													"child-count" to "4",
																												),
																											),
																										),
																										props = mutableMapOf(
																											"res-name" to "type_edit",
																											"visibility" to "VISIBLE",
																											"width" to "1770",
																											"height" to "72",
																											"has-focus" to "false",
																											"has-focusable" to "true",
																											"has-window-focus" to "true",
																											"is-clickable" to "true",
																											"is-enabled" to "true",
																											"is-focused" to "false",
																											"is-focusable" to "true",
																											"is-layout-requested" to "false",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "false",
																											"x" to "0.0",
																											"y" to "0.0",
																											"child-count" to "1",
																										),
																									),
																									ViewNode(
																										level = 11,
																										name = "AppCompatImageButton",
																										id = "2131624104",
																										matches = false,
																										children = mutableListOf(),
																										props = mutableMapOf(
																											"res-name" to "help",
																											"visibility" to "GONE",
																											"width" to "0",
																											"height" to "0",
																											"has-focus" to "false",
																											"has-focusable" to "false",
																											"has-window-focus" to "true",
																											"is-clickable" to "true",
																											"is-enabled" to "true",
																											"is-focused" to "false",
																											"is-focusable" to "true",
																											"is-layout-requested" to "true",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "false",
																											"x" to "0.0",
																											"y" to "0.0",
																										),
																									),
																								),
																								props = mutableMapOf(
																									"visibility" to "VISIBLE",
																									"width" to "1770",
																									"height" to "72",
																									"has-focus" to "false",
																									"has-focusable" to "true",
																									"has-window-focus" to "true",
																									"is-clickable" to "false",
																									"is-enabled" to "true",
																									"is-focused" to "false",
																									"is-focusable" to "false",
																									"is-layout-requested" to "false",
																									"is-selected" to "false",
																									"root-is-layout-requested" to "false",
																									"has-input-connection" to "false",
																									"x" to "0.0",
																									"y" to "384.0",
																									"child-count" to "2",
																								),
																							),
																							ViewNode(
																								level = 10,
																								name = "RecyclerView",
																								id = "16908293",
																								matches = false,
																								children = mutableListOf(),
																								props = mutableMapOf(
																									"res-name" to "hint",
																									"visibility" to "GONE",
																									"width" to "0",
																									"height" to "0",
																									"has-focus" to "false",
																									"has-focusable" to "false",
																									"has-window-focus" to "true",
																									"is-clickable" to "false",
																									"is-enabled" to "true",
																									"is-focused" to "false",
																									"is-focusable" to "true",
																									"is-layout-requested" to "true",
																									"is-selected" to "false",
																									"root-is-layout-requested" to "false",
																									"has-input-connection" to "false",
																									"x" to "0.0",
																									"y" to "0.0",
																									"child-count" to "0",
																								),
																							),
																							ViewNode(
																								level = 10,
																								name = "TextInputLayout",
																								id = "-1",
																								matches = false,
																								children = mutableListOf(
																									ViewNode(
																										level = 11,
																										name = "TextInputEditText",
																										id = "2131623958",
																										matches = false,
																										children = mutableListOf(),
																										props = mutableMapOf(
																											"res-name" to "title",
																											"visibility" to "VISIBLE",
																											"width" to "1770",
																											"height" to "136",
																											"has-focus" to "true",
																											"has-focusable" to "true",
																											"has-window-focus" to "true",
																											"is-clickable" to "true",
																											"is-enabled" to "true",
																											"is-focused" to "true",
																											"is-focusable" to "true",
																											"is-layout-requested" to "false",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "true",
																											// TODO editor-info is wrong
																											"imeOptions" to "0x8000005",
																											"privateImeOptions" to "null",
																											"actionLabel" to "null",
																											"actionId" to "0",
																											"initialSelStart" to "0",
																											"initialSelEnd" to "0",
																											"initialCapsMode" to "0x2000",
																											"hintText" to "Property Title",
																											"label" to "null",
																											"packageName" to "null",
																											"fieldId" to "0",
																											"fieldName" to "null",
																											"extras" to "null ]",
																											"x" to "0.0",
																											"y" to "33.0",
																											"text" to "Test Property",
																											"input-type" to "8193",
																											"ime-target" to "true",
																											"has-links" to "false",
																											"ei-inputType" to "0x200",
																										),
																									),
																								),
																								props = mutableMapOf(
																									"visibility" to "VISIBLE",
																									"width" to "1770",
																									"height" to "169",
																									"has-focus" to "true",
																									"has-focusable" to "true",
																									"has-window-focus" to "true",
																									"is-clickable" to "false",
																									"is-enabled" to "true",
																									"is-focused" to "false",
																									"is-focusable" to "false",
																									"is-layout-requested" to "false",
																									"is-selected" to "false",
																									"root-is-layout-requested" to "false",
																									"has-input-connection" to "false",
																									"x" to "0.0",
																									"y" to "456.0",
																									"child-count" to "1",
																								),
																							),
																							ViewNode(
																								level = 10,
																								name = "TextInputLayout",
																								id = "-1",
																								matches = false,
																								children = mutableListOf(
																									ViewNode(
																										level = 11,
																										name = "TextInputEditText",
																										id = "2131623944",
																										matches = false,
																										children = mutableListOf(),
																										props = mutableMapOf(
																											"res-name" to "description",
																											"visibility" to "VISIBLE",
																											"width" to "1770",
																											"height" to "325",
																											"has-focus" to "false",
																											"has-focusable" to "true",
																											"has-window-focus" to "true",
																											"is-clickable" to "true",
																											"is-enabled" to "true",
																											"is-focused" to "false",
																											"is-focusable" to "true",
																											"is-layout-requested" to "false",
																											"is-selected" to "false",
																											"root-is-layout-requested" to "false",
																											"has-input-connection" to "true",
																											// TODO editor-info is wrong
																											"imeOptions" to "0x44000006",
																											"privateImeOptions" to "null",
																											"actionLabel" to "null",
																											"actionId" to "0",
																											"initialSelStart" to "0",
																											"initialSelEnd" to "0",
																											"initialCapsMode" to "0x0",
																											"hintText" to "Property Description",
																											"label" to "null",
																											"packageName" to "null",
																											"fieldId" to "0",
																											"fieldName" to "null",
																											"extras" to "null ]",
																											"x" to "0.0",
																											"y" to "33.0",
																											"text" to "Test Description\nÁÉÓÖŐÚÜŰ\nماهو الاسم؟",
																											"input-type" to "131073",
																											"ime-target" to "false",
																											"has-links" to "false",
																											"ei-inputType" to "0x2000",
																										),
																									),
																								),
																								props = mutableMapOf(
																									"visibility" to "VISIBLE",
																									"width" to "1770",
																									"height" to "358",
																									"has-focus" to "false",
																									"has-focusable" to "true",
																									"has-window-focus" to "true",
																									"is-clickable" to "false",
																									"is-enabled" to "true",
																									"is-focused" to "false",
																									"is-focusable" to "false",
																									"is-layout-requested" to "false",
																									"is-selected" to "false",
																									"root-is-layout-requested" to "false",
																									"has-input-connection" to "false",
																									"x" to "0.0",
																									"y" to "625.0",
																									"child-count" to "1",
																								),
																							),
																						),
																						props = mutableMapOf(
																							"visibility" to "VISIBLE",
																							"width" to "1770",
																							"height" to "983",
																							"has-focus" to "true",
																							"has-focusable" to "true",
																							"has-window-focus" to "true",
																							"is-clickable" to "false",
																							"is-enabled" to "true",
																							"is-focused" to "false",
																							"is-focusable" to "false",
																							"is-layout-requested" to "false",
																							"is-selected" to "false",
																							"root-is-layout-requested" to "false",
																							"has-input-connection" to "false",
																							"x" to "0.0",
																							"y" to "0.0",
																							"child-count" to "5",
																						),
																					),
																				),
																				props = mutableMapOf(
																					"visibility" to "VISIBLE",
																					"width" to "1770",
																					"height" to "693",
																					"has-focus" to "true",
																					"has-focusable" to "true",
																					"has-window-focus" to "true",
																					"is-clickable" to "false",
																					"is-enabled" to "true",
																					"is-focused" to "false",
																					"is-focusable" to "true",
																					"is-layout-requested" to "false",
																					"is-selected" to "false",
																					"root-is-layout-requested" to "false",
																					"has-input-connection" to "false",
																					"x" to "0.0",
																					"y" to "0.0",
																					"child-count" to "1",
																				),
																			),
																			ViewNode(
																				level = 8,
																				name = "AppCompatButton",
																				id = "2131623942",
																				matches = false,
																				children = mutableListOf(),
																				props = mutableMapOf(
																					"res-name" to "btn_save",
																					"visibility" to "VISIBLE",
																					"width" to "264",
																					"height" to "144",
																					"has-focus" to "false",
																					"has-focusable" to "true",
																					"has-window-focus" to "true",
																					"is-clickable" to "true",
																					"is-enabled" to "true",
																					"is-focused" to "false",
																					"is-focusable" to "true",
																					"is-layout-requested" to "false",
																					"is-selected" to "false",
																					"root-is-layout-requested" to "false",
																					"has-input-connection" to "false",
																					"x" to "753.0",
																					"y" to "693.0",
																					"text" to "Save",
																					"input-type" to "0",
																					"ime-target" to "false",
																					"has-links" to "false",
																				),
																			),
																		),
																		props = mutableMapOf(
																			"res-name" to "fragmentRoot",
																			"visibility" to "VISIBLE",
																			"width" to "1770",
																			"height" to "837",
																			"has-focus" to "true",
																			"has-focusable" to "true",
																			"has-window-focus" to "true",
																			"is-clickable" to "false",
																			"is-enabled" to "true",
																			"is-focused" to "false",
																			"is-focusable" to "false",
																			"is-layout-requested" to "false",
																			"is-selected" to "false",
																			"root-is-layout-requested" to "false",
																			"has-input-connection" to "false",
																			"x" to "12.0",
																			"y" to "12.0",
																			"child-count" to "2",
																		),
																	),
																),
																props = mutableMapOf(
																	"res-name" to "activityRoot",
																	"visibility" to "VISIBLE",
																	"width" to "1794",
																	"height" to "861",
																	"has-focus" to "true",
																	"has-focusable" to "true",
																	"has-window-focus" to "true",
																	"is-clickable" to "false",
																	"is-enabled" to "true",
																	"is-focused" to "false",
																	"is-focusable" to "false",
																	"is-layout-requested" to "false",
																	"is-selected" to "false",
																	"root-is-layout-requested" to "false",
																	"has-input-connection" to "false",
																	"x" to "0.0",
																	"y" to "0.0",
																	"child-count" to "1",
																),
															),
														),
														props = mutableMapOf(
															"visibility" to "VISIBLE",
															"width" to "1794",
															"height" to "861",
															"has-focus" to "true",
															"has-focusable" to "true",
															"has-window-focus" to "true",
															"is-clickable" to "false",
															"is-enabled" to "true",
															"is-focused" to "false",
															"is-focusable" to "false",
															"is-layout-requested" to "false",
															"is-selected" to "false",
															"root-is-layout-requested" to "false",
															"has-input-connection" to "false",
															"x" to "0.0",
															"y" to "0.0",
															"child-count" to "1",
														),
													),
												),
												props = mutableMapOf(
													"res-name" to "content",
													"visibility" to "VISIBLE",
													"width" to "1794",
													"height" to "861",
													"has-focus" to "true",
													"has-focusable" to "true",
													"has-window-focus" to "true",
													"is-clickable" to "false",
													"is-enabled" to "true",
													"is-focused" to "false",
													"is-focusable" to "false",
													"is-layout-requested" to "false",
													"is-selected" to "false",
													"root-is-layout-requested" to "false",
													"has-input-connection" to "false",
													"x" to "0.0",
													"y" to "144.0",
													"child-count" to "1",
												),
											),
											ViewNode(
												level = 4,
												name = "ActionBarContainer",
												id = "2131624055",
												matches = false,
												children = mutableListOf(
													ViewNode(
														level = 5,
														name = "Toolbar",
														id = "2131624056",
														matches = false,
														children = mutableListOf(
															ViewNode(
																level = 6,
																name = "ImageButton",
																id = "-1",
																matches = false,
																children = mutableListOf(),
																props = mutableMapOf(
																	"desc" to "Navigate up",
																	"visibility" to "VISIBLE",
																	"width" to "168",
																	"height" to "144",
																	"has-focus" to "false",
																	"has-focusable" to "false",
																	"has-window-focus" to "true",
																	"is-clickable" to "true",
																	"is-enabled" to "true",
																	"is-focused" to "false",
																	"is-focusable" to "true",
																	"is-layout-requested" to "false",
																	"is-selected" to "false",
																	"root-is-layout-requested" to "false",
																	"has-input-connection" to "false",
																	"x" to "0.0",
																	"y" to "0.0",
																),
															),
															ViewNode(
																level = 6,
																name = "TextView",
																id = "-1",
																matches = false,
																children = mutableListOf(),
																props = mutableMapOf(
																	"visibility" to "VISIBLE",
																	"width" to "256",
																	"height" to "57",
																	"has-focus" to "false",
																	"has-focusable" to "false",
																	"has-window-focus" to "true",
																	"is-clickable" to "false",
																	"is-enabled" to "true",
																	"is-focused" to "false",
																	"is-focusable" to "false",
																	"is-layout-requested" to "false",
																	"is-selected" to "false",
																	"root-is-layout-requested" to "false",
																	"has-input-connection" to "false",
																	"x" to "216.0",
																	"y" to "43.0",
																	"text" to "New Property",
																	"input-type" to "0",
																	"ime-target" to "false",
																	"has-links" to "false",
																),
															),
															ViewNode(
																level = 6,
																name = "ActionMenuView",
																id = "-1",
																matches = false,
																children = mutableListOf(
																	ViewNode(
																		level = 7,
																		name = "ActionMenuItemView",
																		id = "2131624205",
																		matches = false,
																		children = mutableListOf(),
																		props = mutableMapOf(
																			"res-name" to "action_picture_get",
																			"desc" to "Take Picture",
																			"visibility" to "VISIBLE",
																			"width" to "168",
																			"height" to "144",
																			"has-focus" to "false",
																			"has-focusable" to "false",
																			"has-window-focus" to "true",
																			"is-clickable" to "true",
																			"is-enabled" to "true",
																			"is-focused" to "false",
																			"is-focusable" to "true",
																			"is-layout-requested" to "false",
																			"is-selected" to "false",
																			"root-is-layout-requested" to "false",
																			"has-input-connection" to "false",
																			"x" to "0.0",
																			"y" to "0.0",
																			"text" to "",
																			"input-type" to "0",
																			"ime-target" to "false",
																			"has-links" to "false",
																		),
																	),
																	ViewNode(
																		level = 7,
																		name = "OverflowMenuButton",
																		id = "-1",
																		matches = false,
																		children = mutableListOf(),
																		props = mutableMapOf(
																			"desc" to "More options",
																			"visibility" to "VISIBLE",
																			"width" to "108",
																			"height" to "144",
																			"has-focus" to "false",
																			"has-focusable" to "false",
																			"has-window-focus" to "true",
																			"is-clickable" to "true",
																			"is-enabled" to "true",
																			"is-focused" to "false",
																			"is-focusable" to "true",
																			"is-layout-requested" to "false",
																			"is-selected" to "false",
																			"root-is-layout-requested" to "false",
																			"has-input-connection" to "false",
																			"x" to "168.0",
																			"y" to "0.0",
																		),
																	),
																),
																props = mutableMapOf(
																	"visibility" to "VISIBLE",
																	"width" to "276",
																	"height" to "144",
																	"has-focus" to "false",
																	"has-focusable" to "false",
																	"has-window-focus" to "true",
																	"is-clickable" to "false",
																	"is-enabled" to "true",
																	"is-focused" to "false",
																	"is-focusable" to "false",
																	"is-layout-requested" to "false",
																	"is-selected" to "false",
																	"root-is-layout-requested" to "false",
																	"has-input-connection" to "false",
																	"x" to "1518.0",
																	"y" to "0.0",
																	"child-count" to "2",
																),
															),
														),
														props = mutableMapOf(
															"res-name" to "action_bar",
															"visibility" to "VISIBLE",
															"width" to "1794",
															"height" to "144",
															"has-focus" to "false",
															"has-focusable" to "false",
															"has-window-focus" to "true",
															"is-clickable" to "false",
															"is-enabled" to "true",
															"is-focused" to "false",
															"is-focusable" to "false",
															"is-layout-requested" to "false",
															"is-selected" to "false",
															"root-is-layout-requested" to "false",
															"has-input-connection" to "false",
															"x" to "0.0",
															"y" to "0.0",
															"child-count" to "3",
														),
													),
													ViewNode(
														level = 5,
														name = "ActionBarContextView",
														id = "2131624057",
														matches = false,
														children = mutableListOf(),
														props = mutableMapOf(
															"res-name" to "action_context_bar",
															"visibility" to "GONE",
															"width" to "0",
															"height" to "0",
															"has-focus" to "false",
															"has-focusable" to "false",
															"has-window-focus" to "true",
															"is-clickable" to "false",
															"is-enabled" to "true",
															"is-focused" to "false",
															"is-focusable" to "false",
															"is-layout-requested" to "true",
															"is-selected" to "false",
															"root-is-layout-requested" to "false",
															"has-input-connection" to "false",
															"x" to "0.0",
															"y" to "0.0",
															"child-count" to "0",
														),
													),
												),
												props = mutableMapOf(
													"res-name" to "action_bar_container",
													"visibility" to "VISIBLE",
													"width" to "1794",
													"height" to "144",
													"has-focus" to "false",
													"has-focusable" to "false",
													"has-window-focus" to "true",
													"is-clickable" to "false",
													"is-enabled" to "true",
													"is-focused" to "false",
													"is-focusable" to "false",
													"is-layout-requested" to "false",
													"is-selected" to "false",
													"root-is-layout-requested" to "false",
													"has-input-connection" to "false",
													"x" to "0.0",
													"y" to "0.0",
													"child-count" to "2",
												),
											),
										),
										props = mutableMapOf(
											"res-name" to "decor_content_parent",
											"visibility" to "VISIBLE",
											"width" to "1794",
											"height" to "1005",
											"has-focus" to "true",
											"has-focusable" to "true",
											"has-window-focus" to "true",
											"is-clickable" to "false",
											"is-enabled" to "true",
											"is-focused" to "false",
											"is-focusable" to "false",
											"is-layout-requested" to "false",
											"is-selected" to "false",
											"root-is-layout-requested" to "false",
											"has-input-connection" to "false",
											"x" to "0.0",
											"y" to "0.0",
											"child-count" to "2",
										),
									),
								),
								props = mutableMapOf(
									"visibility" to "VISIBLE",
									"width" to "1794",
									"height" to "1005",
									"has-focus" to "true",
									"has-focusable" to "true",
									"has-window-focus" to "true",
									"is-clickable" to "false",
									"is-enabled" to "true",
									"is-focused" to "false",
									"is-focusable" to "false",
									"is-layout-requested" to "false",
									"is-selected" to "false",
									"root-is-layout-requested" to "false",
									"has-input-connection" to "false",
									"x" to "0.0",
									"y" to "75.0",
									"child-count" to "1",
								),
							),
						),
						props = mutableMapOf(
							"visibility" to "VISIBLE",
							"width" to "1794",
							"height" to "1080",
							"has-focus" to "true",
							"has-focusable" to "true",
							"has-window-focus" to "true",
							"is-clickable" to "false",
							"is-enabled" to "true",
							"is-focused" to "false",
							"is-focusable" to "false",
							"is-layout-requested" to "false",
							"is-selected" to "false",
							"root-is-layout-requested" to "false",
							"has-input-connection" to "false",
							"x" to "0.0",
							"y" to "0.0",
							"child-count" to "2",
						),
					),
				),
				props = mutableMapOf(
					"visibility" to "VISIBLE",
					"width" to "1794",
					"height" to "1080",
					"has-focus" to "true",
					"has-focusable" to "true",
					"has-window-focus" to "true",
					"is-clickable" to "false",
					"is-enabled" to "true",
					"is-focused" to "false",
					"is-focusable" to "false",
					"is-layout-requested" to "false",
					"is-selected" to "false",
					"root-is-layout-requested" to "false",
					"has-input-connection" to "false",
					"x" to "0.0",
					"y" to "0.0",
					"child-count" to "1",
				),
			),
		)

		assertEquals(expected, actual)
	}
}
