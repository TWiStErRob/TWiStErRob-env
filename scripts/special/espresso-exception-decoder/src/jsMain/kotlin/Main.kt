import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*

sealed class ExceptionResult

data class ViewExceptionResult(
	val ex: String,
	val matcher: String,
	val marker: String,
	val hierarchyText: String,
	var hierarchyArray: List<String> = emptyList(),
	var stacktrace: String? = null,
	var hierarchy: ViewNode? = null,
	var resNames: List<String> = emptyList(),
	var types: List<String> = emptyList()
) : ExceptionResult()

data class DataExceptionResult(
	val ex: String,
	val matcher: String,
	val dataText: String,
	var data: MutableList<DataNode> = mutableListOf()
) : ExceptionResult()

data class RootExceptionResult(
	val ex: String,
	val matcher: String,
	val marker: String,
	val rootText: String,
	var roots: MutableList<RootNode> = mutableListOf()
) : ExceptionResult()


sealed interface TreeNode {
	val name: String
	val children: MutableList<ViewNode>
	var parent: ViewNode?
	val props: MutableMap<String, Any>
}

data class RootNode(
	override val name: String,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
	override var parent: ViewNode? = null,
) : TreeNode

data class ViewNode(
	val level: Int,
	override val name: String,
	val id: String,
	val matches: Boolean,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
	override var parent: ViewNode? = null,
) : TreeNode

data class DataNode(
	override val name: String,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
	override var parent: ViewNode? = null,
) : TreeNode {
}

fun parse(exception: String): ExceptionResult {
	console.log("parsing")
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

	val runtimeRe =
		Regex("""[\s\S]*(java.lang.RuntimeException): No data found matching: \(?(.*?)\)? contained values: \<\[([\s\S]*?)\]\>([\s\S]*)""")
	runtimeRe.find(exception)?.let {
		return parseDataException(
			DataExceptionResult(
				ex = it.groupValues[1],
				matcher = it.groupValues[2],
				dataText = it.groupValues[3],
			)
		)
	}

	throw Exception("Cannot match $exception")
}

fun parseViewException(result: ViewExceptionResult): ViewExceptionResult {
	result.hierarchyArray = result.hierarchyText.split("\n|\n")
	val last = result.hierarchyArray.last()
	println(last)
	if (Regex("""^\s*at""").containsMatchIn(last)) {
		result.stacktrace = last // TODO stack trace contains stuff after the "at " lines
		result.hierarchyArray = result.hierarchyArray.dropLast(1)
	} else {
		result.stacktrace = null
	}
	val views = toViews(result.hierarchyArray, result.marker)
	result.hierarchy = build(views)
	result.resNames = collect(views, "res-name")
	result.types = collect(views, "name")
	return result
}

fun parseDataException(result: DataExceptionResult): DataExceptionResult {
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

fun parseRootException(result: RootExceptionResult): RootExceptionResult {
	result.roots = mutableListOf()
	val dataRe = Regex("""Root\{(?:application-window-token=(.*?), window-token=(.*?), has-window-focus=(.*?), (?:layout-params-type=(.*?), )?(?:layout-params-string=(.*?), )?decor-view-string=(.*?\}))\}""")
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
			val paramsMatch = Regex("""(.*?)\{\((\d+),(\d+)\)\((\d+|fill|wrap)x(\d+|fill|wrap)\) (.*)\}""").find(match.groups[5]!!.value)!!
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

fun collect(arr: List<ViewNode>, prop: String): List<String> {
	val set = mutableSetOf<String>()
	for (view in arr) {
		view.props[prop]?.let { set.add(it as String) }
	}
	return set.sorted()
}
/*
function escapeRegExp(str) {
	// noinspection *
	return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
}
*/
fun parseView(text: String, marker: String): ViewNode {
	val regex = Regex("^\\+(-*)>(.*?)\\{id=(-?\\d+), ([\\s\\S]+)\\}( " + Regex.escape(marker) + ")?$")
	val match = regex.find(text) ?: throw Exception("Cannot parse view: $text")
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

fun parseProps(map: MutableMap<String, Any>, rest: String): MutableMap<String, Any> {
	val propRe = Regex("""(.*?)[=:]([\s\S]*?)(,? (?=[\w-]+[=:])|$)""")
	propRe.findAll(rest).forEach { match ->
		map[match.groups[1]!!.value] = match.groups[2]!!.value
	}
	return map
}

fun toViews(input: List<String>, marker: String): List<ViewNode> = 
	input.map { parseView(it, marker) }

fun build(views: List<ViewNode>): ViewNode {
	val stack = mutableListOf<ViewNode>()
	for (view in views) {
		if (view.level > stack.size) {
			error("${view} is deeper than ${stack.size}: missing some parents.")
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

fun render(error: ExceptionResult) {
	when (error) {
		is ViewExceptionResult -> renderHierarchy(error)
		is DataExceptionResult -> renderData(error)
		is RootExceptionResult -> renderRoots(error)
		/*
		throw 'Cannot render ' + JSON.stringify(error);
		 */
	}
}

/*
function renderRoots(error) {
*/
fun renderRoots(error: RootExceptionResult) {
	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	for (root in error.roots) {
		root.props["_display"] = document.createElement("span")
		for (child in root.children) {
			child.props["_display"] = document.createElement("span").toString()
		}

		val hierarchyTreeRootDomLi = document.createElement("li") as HTMLLIElement
		renderTree(hierarchyTreeRootDomLi, root)
		hierarchyTreeRootDomUl.appendChild(hierarchyTreeRootDomLi)
	}
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}
/*
	function renderData(error) {
 */
fun renderData(error: DataExceptionResult) {
	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	for (d in error.data) {
		d.props["_display"] = document.createElement("span")

		val hierarchyTreeRootDomLi = document.createElement("li") as HTMLLIElement
		renderTree(hierarchyTreeRootDomLi, d)
		hierarchyTreeRootDomUl.appendChild(hierarchyTreeRootDomLi)
	}
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}
/*
	function renderHierarchy(error) {
 */
fun renderHierarchy(error: ViewExceptionResult) {
	val h = error.hierarchy!!

	val messageDom = document.getElementById("message") as HTMLElement
	messageDom.asDynamic().error = error
	messageDom.innerHTML = "<b>${error.ex}</b>: ${error.matcher}"

	val hierarchyDom = document.getElementById("hierarchy-display") as HTMLElement
	while (hierarchyDom.firstChild != null) {
		hierarchyDom.removeChild(hierarchyDom.firstChild!!)
	}
	val width = (h.props["width"] as String).toFloat()
	val height = (h.props["height"] as String).toFloat()
	hierarchyDom.style.paddingBottom = "${height / width * 100}%"
	renderView(hierarchyDom, h, 100 / width, 100 / height)
	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	val hierarchyTreeRootDomLi = document.createElement("li") as HTMLLIElement
	renderTree(hierarchyTreeRootDomLi, h)
	hierarchyTreeRootDomUl.appendChild(hierarchyTreeRootDomLi)
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}

/*
	String.prototype.hashCode = function () {
*/
fun String.hashCode32(): Int {
	var hash = 0
	for (element in this) {
		val chr = element.code
		hash = ((hash shl 5) - hash) + chr
		hash = hash or 0 // Convert to 32bit integer
	}
	return hash
}

/*
	function renderView(target, view, scaleX, scaleY) {
*/
fun renderView(target: HTMLElement, view: ViewNode, scaleX: Float, scaleY: Float) {
	val dom = document.createElement("div") as HTMLElement
	view.props["_display"] = dom
	dom.asDynamic().view = view
	dom.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(event.currentTarget.asDynamic().view)
	})
	dom.className = listOf("view", view.name, view.props["visibility"], if (view.matches) "MATCHES" else "").joinToString(" ")
	dom.style.width = "${(view.props["width"] as String).toFloat() * scaleX}%"
	dom.style.height = "${(view.props["height"] as String).toFloat() * scaleY}%"
	dom.style.left = "${(view.props["x"] as String).toFloat() * scaleX}%"
	dom.style.top = "${(view.props["y"] as String).toFloat() * scaleY}%"
	dom.style.backgroundColor = dom.className.hashCode32().toString(16)
	dom.setAttribute("data-type", view.name)
	(view.props["text"] as String?)?.let { text ->
		dom.innerText = text
	}
	target.appendChild(dom)
	for (child in view.children) {
		renderView(dom, child, 100 / (view.props["width"] as String).toFloat(), 100 / (view.props["height"] as String).toFloat())
	}
}

/*
	function renderTree(target, view) {
 */
fun renderTree(target: HTMLElement, view: TreeNode) {
	view.props["_tree"] = target
	target.asDynamic().view = view
	target.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(event.currentTarget.asDynamic().view)
	})
	target.className = listOf("view", view.name, view.props["visibility"], if (view is ViewNode && view.matches) "MATCHES" else "").joinToString(" ")
	target.setAttribute("data-type", view.name)
	val name = document.createElement("span") as HTMLElement
	name.innerText = view.name
	name.className = "name"
	target.appendChild(name)
	if (view.children.isNotEmpty()) {
		val children = document.createElement("ul") as HTMLUListElement
		for (childView in view.children) {
			val child = document.createElement("li") as HTMLLIElement
			renderTree(child, childView)
			children.appendChild(child)
		}
		target.appendChild(children)
	}
}

/*
	function parents(view) {
*/
fun parents(view: TreeNode): List<String> {
	val parents = mutableListOf<String>()
	var v = view.parent
	while (v != null) {
		val resName = v.props["res-name"]
		parents.add(v.name + (if (resName != null) " (${resName})" else ""))
		v = v.parent
	}
	return parents.reversed()
}

var lastView: TreeNode? = null

/*
	function showView(view) {
 */
fun showView(view: TreeNode?) {
	if (lastView != null) {
		(lastView!!.props["_display"] as HTMLElement).classList.remove("highlight")
		(lastView!!.props["_tree"] as HTMLElement).classList.remove("highlight")
	}
	lastView = view
	if (view == null) return
	(view.props["_display"] as HTMLElement).classList.add("highlight")
	(view.props["_tree"] as HTMLElement).classList.add("highlight")

	(document.getElementById("name") as HTMLHeadingElement).innerText = view.name
	(document.getElementById("path") as HTMLHeadingElement).innerText = "in " + parents(view).joinToString(" > ")

	val props = document.getElementById("properties") as HTMLElement
	while (props.firstChild != null) {
		props.removeChild(props.firstChild!!)
	}
	val exclusions = setOf("children", "parent", "name", "matches", "_display", "_tree")
	val propsKeysToShow = view.props.keys.filter { it !in exclusions }
	val priorities = mapOf(
		"^id" to 10,
		"^token" to 10,
		"^res-name" to 11,
		"^matches" to 12,
		"^name" to 20,
		"^class" to 20,
		"^text" to 30,
		"^data" to 31,
		"^desc" to 31,
		"^level" to 40,
		"^width" to 41,
		"^height" to 42,
		"^x" to 43,
		"^y" to 44,
		"^ei-.*" to Int.MAX_VALUE
	)

	fun getPriority(x: String): Int {
		for ((re, priority) in priorities) {
			if (Regex(re).matches(x)) {
				return priority
			}
		}
		return 100000
	}

	val propsToShow = propsKeysToShow.sortedWith(compareBy({ getPriority(it) }, { it }))
	for (prop in propsToShow) {
		val propName = document.createElement("dt") as HTMLElement
		val propValue = document.createElement("dd") as HTMLElement
		propName.innerText = prop
		propValue.innerText = view.props[prop] as String
		props.appendChild(propName)
		props.appendChild(propValue)
	}
}
/*
</script>
 */
fun main() {
	/*
<script>
	 */
	window.onload = {
		val output = document.getElementById("output-container") as HTMLElement
		val input = document.getElementById("trace") as HTMLTextAreaElement
		val message = document.getElementById("message") as HTMLElement
		val border = document.getElementById("border-type") as HTMLSelectElement
		val names = document.getElementById("name-display") as HTMLInputElement

		border.addEventListener("change", {
			for (i in 0 until border.options.length) {
				val option = border.options.item(i) as HTMLOptionElement
				val optionClasses = option.value.split(" ")
				for (c in optionClasses) {
					output.classList.remove(c)
				}
			}
			val classes = border.value.split(" ")
			for (c in classes) {
				output.classList.add(c)
			}
		})
		input.addEventListener("input", {
			render(parse(input.value))
		})
		render(parse(input.value))
		names.addEventListener("change", {
			if (names.checked) {
				output.classList.add("show-names")
			} else {
				output.classList.remove("show-names")
			}
		})
		message.addEventListener("click", {
			document.getElementById("name")!!.textContent = "Statistics"
			document.getElementById("path")!!.textContent = ""
			val error = message.asDynamic().error
			document.getElementById("properties")!!.innerHTML =
				"""
					<dt>Available res-names</dt>
					<dd>${error.resNames.join(", ")}</dd>
					<dt>Available classes</dt>
					<dd>${error.types.join(", ")}</dd>
				""".trimIndent()
		})
	}
}
