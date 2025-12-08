@file:Suppress("detekt.MagicNumber")

package net.twisterrob.android.testing.parser

import net.twisterrob.android.testing.DataExceptionResult
import net.twisterrob.android.testing.renderer.DataNode
import net.twisterrob.android.testing.ExceptionResult
import net.twisterrob.android.testing.RootExceptionResult
import net.twisterrob.android.testing.renderer.RootNode
import net.twisterrob.android.testing.ViewExceptionResult
import net.twisterrob.android.testing.renderer.ViewNode
import kotlin.collections.iterator

@Suppress("RegExpRedundantEscape", "detekt.ReturnCount")
internal fun parse(exception: String): ExceptionResult {
	println("parsing")
	@Suppress("detekt.MaxLineLength")
	val ambiguousRe =
		Regex("""[\s\S]*(androidx\.test\.espresso\.AmbiguousViewMatcherException|android\.support\.test\.espresso\.AmbiguousViewMatcherException): '(.*?)' matches multiple views in the hierarchy\.\nProblem views are marked with '(\*\*\*\*MATCHES\*\*\*\*)' below.\n\nView Hierarchy:\n([\s\S]*)""")

	ambiguousRe.find(exception)?.let {
		return parseViewException(
			ViewExceptionResult(
				ex = it.groupValues[1],
				matcher = it.groupValues[2],
				marker = it.groupValues[3],
				hierarchyText = it.groupValues[4],
			)
		)
	}

	@Suppress("detekt.MaxLineLength")
	val noMatchRe =
		Regex("""[\s\S]*(androidx.test.espresso.NoMatchingViewException|android.support.test.espresso.NoMatchingViewException): No views in hierarchy found matching: \(?(.*?)\)?\n[\s\S]*View Hierarchy:\n([\s\S]*)""")
	noMatchRe.find(exception)?.let {
		return parseViewException(
			ViewExceptionResult(
				ex = it.groupValues[1],
				matcher = it.groupValues[2],
				marker = "",
				hierarchyText = it.groupValues[3],
			)
		)
	}

	@Suppress("detekt.MaxLineLength")
	val noRootRe =
		Regex("""[\s\S]*(androidx.test.espresso.NoMatchingRootException|android.support.test.espresso.NoMatchingRootException): Matcher '([\s\S]*?)' did not match any of the following roots: \[([\s\S]*)\]\n([\s\S]*)""")
	noRootRe.find(exception)?.let {
		return parseRootException(
			RootExceptionResult(
				ex = it.groupValues[1],
				matcher = it.groupValues[2],
				marker = "",
				rootText = it.groupValues[3],
			)
		)
	}

	@Suppress("detekt.MaxLineLength")
	val runtimeRe =
		Regex("""[\s\S]*(java.lang.RuntimeException): No data found matching: \(?(.*?)\)? contained values: <\[([\s\S]*?)\]>([\s\S]*)""")
	runtimeRe.find(exception)?.let {
		return parseDataException(
			DataExceptionResult(
				ex = it.groupValues[1],
				matcher = it.groupValues[2],
				dataText = it.groupValues[3],
			)
		)
	}

	error("Cannot match $exception")
}

private fun parseViewException(result: ViewExceptionResult): ViewExceptionResult {
	result.hierarchyArray = result.hierarchyText.split("\n|\n")
	val last = result.hierarchyArray.last()
	println(last)
	if (Regex("""^\s*at""").containsMatchIn(last)) {
		result.stacktrace = last // TODO stack trace contains stuff after the "at " lines
		result.hierarchyArray = result.hierarchyArray.dropLast(1)
	} else {
		result.stacktrace = null
	}
	val views = result.hierarchyArray.map { parseView(it, result.marker) }
	result.hierarchy = build(views)
	result.resNames = collect(views, "res-name")
	result.types = collect(views, "name")
	return result
}

private fun parseDataException(result: DataExceptionResult): DataExceptionResult {
	result.data = mutableListOf()
	val dataRe = Regex("""Data: (.*?) \(class: (.*?)\) token: (\d+)(?:, |$)""")
	dataRe.findAll(result.dataText).forEach { match ->
		val row = DataNode(
			name = match.groups[2]!!.value,
			props = mutableMapOf(
				"token" to match.groups[3]!!.value,
				"class" to match.groups[2]!!.value,
				"data" to match.groups[1]!!.value,
			)
		)
		if (row.props["class"] == "android.database.sqlite.SQLiteCursor") {
			val rowMatch = Regex("""Row (\d+): \{(.*)\}""").find(row.props["data"] as String)!!
			val props = parseProps(mutableMapOf("row #" to rowMatch.groups[1]!!.value), rowMatch.groups[2]!!.value)
			for ((k, v) in props) {
				row.props["data-${k}"] = v
			}
		}
		result.data.add(row)
	}
	return result
}

private fun parseRootException(result: RootExceptionResult): RootExceptionResult {
	result.roots = mutableListOf()
	@Suppress(
		"detekt.MaxLineLength",
		"RegExpUnnecessaryNonCapturingGroup",  // Otherwise it's capturing!
	)
	val dataRe =
		Regex("""Root\{(?:application-window-token=(.*?), window-token=(.*?), has-window-focus=(.*?), (?:layout-params-type=(.*?), )?(?:layout-params-string=(.*?), )?decor-view-string=(.*?\}))\}""")
	dataRe.findAll(result.rootText).forEach { match ->
		val root = RootNode(
			name = match.groups[2]!!.value,
			props = mutableMapOf(
				"application-window-token" to match.groups[1]!!.value,
				"window-token" to match.groups[2]!!.value,
				"has-window-focus" to match.groups[3]!!.value,
				//"decor-view-string": match.groups[6]!!.value,
			),
		)
		if (match.groups[4] != null) {
			root.props["layout-params-type"] = match.groups[4]!!.value
			//root["layout-params-string"] = match.groups[5]!!.value
			val paramsMatch =
				Regex("""(.*?)\{\((\d+),(\d+)\)\((\d+|fill|wrap)x(\d+|fill|wrap)\) (.*)\}""").find(match.groups[5]!!.value)!!
			val params = mutableMapOf<String, Any>(
				"name" to paramsMatch.groups[1]!!.value,
				"x" to paramsMatch.groups[2]!!.value,
				"y" to paramsMatch.groups[3]!!.value,
				"width" to paramsMatch.groups[4]!!.value,
				"height" to paramsMatch.groups[5]!!.value,
			)
			parseProps(params, paramsMatch.groups[6]!!.value)
			for ((k, v) in params) {
				root.props[/*"layout-params-" +*/ k] = v
			}
			root.props["layout-params-class"] = params["name"]!!
			//root.children.add(params)
		}
		root.children.add(parseView("+>${match.groups[6]?.value}", ""))
		result.roots.add(root)
	}
	return result
}

private fun parseView(text: String, marker: String): ViewNode {
	val regex = Regex("^\\+(-*)>(.*?)\\{id=(-?\\d+), ([\\s\\S]+)\\}( " + Regex.escape(marker) + ")?$")
	val match = regex.find(text) ?: error("Cannot net.twisterrob.android.testing.parser.parse view: $text")
	val view = ViewNode(
		level = match.groups[1]!!.value.length,
		name = match.groups[2]!!.value,
		id = match.groups[3]!!.value,
		matches = match.groups[5] != null,
		children = mutableListOf(),
	)
	val rest = match.groups[4]!!.value
	parseProps(view.props, rest)
	view.props["editor-info"]?.let { info ->
		info as String
		view.props.remove("editor-info")
		val infoStr = info.substring(1, info.length - 1)
		val infoRe = Regex("""(.*?)=(.*?)( (?=\w+=)|$)""")
		infoRe.findAll(infoStr).forEach { match ->
			view.props["ei-${match.groups[1]!!.value}"] = match.groups[2]!!.value
		}
	}
	return view
}

private fun parseProps(map: MutableMap<String, Any>, rest: String): MutableMap<String, Any> {
	val propRe = Regex("""(.*?)[=:]([\s\S]*?)(,? (?=[\w-]+[=:])|$)""")
	propRe.findAll(rest).forEach { match ->
		map[match.groups[1]!!.value] = match.groups[2]!!.value
	}
	return map
}

private fun build(views: List<ViewNode>): ViewNode {
	val stack = mutableListOf<ViewNode>()
	for (view in views) {
		if (view.level > stack.size) {
			error("${view} is deeper than ${stack.size}: missing some net.twisterrob.android.testing.parents.")
		} else while (view.level < stack.size) {
			//console.log("Popping " + view.name);
			stack.removeLast()
		}
		val parent = stack.removeLastOrNull()
		if (parent != null) {
			//console.log("Parent of " + view.name + " is " + parent.name);
			parent.children.add(view)
			view.parent = parent
			stack.add(parent)
		}
		if (view.level == stack.size) {
			//console.log("Pushing " + view.name);
			stack.add(view)
		}
	}
	return views.first()
}

private fun collect(arr: List<ViewNode>, prop: String): List<String> {
	val set = mutableSetOf<String>()
	for (view in arr) {
		view.props[prop]?.let { set.add(it as String) }
	}
	return set.sorted()
}
