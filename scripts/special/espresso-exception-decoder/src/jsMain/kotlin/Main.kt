import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*

fun main() {
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
	val data: MutableList<Map<String, String>> = mutableListOf()
) : ExceptionResult()

data class RootExceptionResult(
	val ex: String,
	val matcher: String,
	val marker: String,
	val rootText: String,
	val roots: MutableList<Map<String, Any>> = mutableListOf()
) : ExceptionResult()

// View node for hierarchy
data class ViewNode(
	val level: Int,
	val name: String,
	val id: String,
	val matches: Boolean,
	val children: MutableList<ViewNode> = mutableListOf(),
	val props: MutableMap<String, String> = mutableMapOf(),
	var parent: ViewNode? = null
)

fun parse(exception: String): ExceptionResult {
	console.log("parsing")
	val ambiguousRe =
		Regex("""[\s\S]*(android\.support\.test\.espresso\.AmbiguousViewMatcherException): '(.*?)' matches multiple views in the hierarchy\.\nProblem views are marked with '(\*\*\*\*MATCHES\*\*\*\*)' below.\n\nView Hierarchy:\n([\s\S]*)""")

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
		Regex("""[\s\S]*(android.support.test.espresso.NoMatchingViewException): No views in hierarchy found matching: \(?(.*?)\)?\n[\s\S]*View Hierarchy:\n([\s\S]*)""")
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
		Regex("""[\s\S]*(android.support.test.espresso.NoMatchingRootException): Matcher '([\s\S]*?)' did not match any of the following roots: \[([\s\S]*)\]\n([\s\S]*)""")
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
	val last = result.hierarchyArray.lastOrNull()
	if (last != null && Regex("^\\s*at").matches(last.trim())) {
		result.stacktrace = last
		result.hierarchyArray = result.hierarchyArray.dropLast(1)
	} else {
		result.stacktrace = null
	}
	val views = toViews(result.hierarchyArray, result.marker)
	result.hierarchy = buildHierarchy(views)
	result.resNames = collect(views) { it.props["res-name"] }
	result.types = collect(views) { it.name }
	return result
}

fun parseDataException(result: DataExceptionResult): DataExceptionResult {
	val dataRe = Regex("Data: (.*?) \\(class: (.*?)\\) token: (\\d+)(?:, |$)")
	dataRe.findAll(result.dataText).forEach { match ->
		val (data, clazz, token) = match.destructured
		val row = mutableMapOf(
			"token" to token,
			"class" to clazz,
			"data" to data
		)
		if (clazz == "android.database.sqlite.SQLiteCursor") {
			val rowRe = Regex("Row (\\d+): \\{(.*)\\}")
			val rowMatch = rowRe.find(data)
			if (rowMatch != null) {
				val (rowNum, props) = rowMatch.destructured
				val parsedProps = mutableMapOf<String, String>()
				parsedProps["row #"] = rowNum
				parseProps(parsedProps, props)
				for ((k, v) in parsedProps) {
					row["data-$k"] = v
				}
			}
		}
		result.data.add(row)
	}
	return result
}

fun parseRootException(result: RootExceptionResult): RootExceptionResult {
	val dataRe =
		Regex("Root\\{(?:application-window-token=(.*?), window-token=(.*?), has-window-focus=(.*?), (?:layout-params-type=(.*?), )?(?:layout-params-string=(.*?), )?decor-view-string=(.*?\\}))\\}")
	dataRe.findAll(result.rootText).forEach { match ->
		val root = mutableMapOf<String, Any>(
			"application-window-token" to (match.groups[1]?.value ?: ""),
			"window-token" to (match.groups[2]?.value ?: ""),
			"has-window-focus" to (match.groups[3]?.value ?: "")
		)
		root["children"] = mutableListOf<Any>()

		if (match.groups[4]?.value != null) {
			root["layout-params-type"] = match.groups[4]?.value ?: ""
			val paramsMatch = Regex("(.*?)\\{\\((\\d+),(\\d+)\\)\\((\\d+|fill|wrap)x(\\d+|fill|wrap)\\) (.*)\\}").find(
				match.groups[5]?.value ?: ""
			)
			if (paramsMatch != null) {
				val (name, x, y, width, height, propsStr) = paramsMatch.destructured
				val params = mutableMapOf<String, String>(
					"name" to name,
					"x" to x,
					"y" to y,
					"width" to width,
					"height" to height
				)
				parseProps(params, propsStr)
				for ((k, v) in params) {
					root[k] = v
				}
				root["layout-params-class"] = name
			}
		}

		// Parse decor-view-string with +> prefix
		val decorViewString = match.groups[6]?.value ?: ""
		(root["children"] as MutableList<Any>).add(parseView("+>$decorViewString", ""))
		result.roots.add(root)
	}
	return result
}

fun collect(views: List<ViewNode>, propSelector: (ViewNode) -> String?): List<String> {
	val set = mutableSetOf<String>()
	for (view in views) {
		propSelector(view)?.let { set.add(it) }
	}
	return set.sorted()
}

fun parseView(text: String, marker: String): ViewNode {
	val regex = Regex("^\\+(-*)>(.*?)\\{id=(-?\\d+), ([\\s\\S]+)\\}( " + Regex.escape(marker) + ")?")
	val match = regex.matchEntire(text) ?: throw Exception("Cannot parse view: $text")
	val level = match.groups[1]?.value?.length ?: 0
	val name = match.groups[2]?.value ?: ""
	val id = match.groups[3]?.value ?: ""
	val rest = match.groups[4]?.value ?: ""
	val matches = match.groups[6]?.value != null
	val view = ViewNode(level, name, id, matches)
	parseProps(view.props, rest)
	// Editor info parsing
	view.props["editor-info"]?.let { info ->
		val infoStr = info.substring(1, info.length - 1)
		val infoRe = Regex("(.*?)=(.*?)( (?=\\w+=)|$)")
		infoRe.findAll(infoStr).forEach { m ->
			val (k, v) = m.destructured
			view.props["ei-$k"] = v
		}
		view.props.remove("editor-info")
	}
	return view
}

fun parseProps(map: MutableMap<String, Any>, rest: String) {
	val propRe = Regex("(.*?)[=:]([\\s\\S]*?)(,? (?=[\\w-]+[=:])|$)")
	propRe.findAll(rest).forEach { match ->
		val (k, v) = match.destructured
		map[k] = v
	}
}

fun parseProps(map: MutableMap<String, String>, rest: String) {
	val propRe = Regex("(.*?)[=:]([\\s\\S]*?)(,? (?=[\\w-]+[=:])|$)")
	propRe.findAll(rest).forEach { match ->
		val (k, v) = match.destructured
		map[k] = v
	}
}

fun toViews(input: List<String>, marker: String): List<ViewNode> {
	return input.map { parseView(it, marker) }
}

fun buildHierarchy(views: List<ViewNode>): ViewNode? {
	val stack = mutableListOf<ViewNode>()
	for (view in views) {
		while (view.level < stack.size) {
			stack.removeAt(stack.size - 1)
		}
		val parent = stack.lastOrNull()
		if (parent != null) {
			parent.children.add(view)
			view.parent = parent
		}
		if (view.level == stack.size) {
			stack.add(view)
		}
	}
	return views.firstOrNull()
}

fun render(error: ExceptionResult) {
	when (error) {
		is ViewExceptionResult -> renderHierarchy(error)
		is DataExceptionResult -> renderData(error)
		is RootExceptionResult -> renderRoots(error)
	}
}

fun renderHierarchy(error: ViewExceptionResult) {
	val h = error.hierarchy ?: return
	val messageDom = document.getElementById("message") as HTMLElement
	messageDom.asDynamic().error = error
	messageDom.innerHTML = "<b>${error.ex}</b>: ${error.matcher}"

	val hierarchyDom = document.getElementById("hierarchy-display") as HTMLElement
	while (hierarchyDom.firstChild != null) {
		hierarchyDom.removeChild(hierarchyDom.firstChild!!)
	}
	val width = h.props["width"]?.toFloatOrNull() ?: 100f
	val height = h.props["height"]?.toFloatOrNull() ?: 100f
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

fun renderData(error: DataExceptionResult) {
	val messageDom = document.getElementById("message") as HTMLElement
	messageDom.asDynamic().error = error
	messageDom.innerHTML = "<b>${error.ex}</b>: ${error.matcher}"

	val hierarchyDom = document.getElementById("hierarchy-display") as HTMLElement
	while (hierarchyDom.firstChild != null) {
		hierarchyDom.removeChild(hierarchyDom.firstChild!!)
	}

	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	for (d in error.data) {
		// Create a dynamic object for data node
		val dataNode: dynamic = js("{}")
		dataNode.name = d["class"] ?: ""
		dataNode.children = js("[]")
		dataNode._display = document.createElement("span")

		// Copy all properties from the map
		for ((key, value) in d) {
			dataNode[key] = value
		}

		val li = document.createElement("li") as HTMLLIElement
		li.asDynamic().view = dataNode
		renderTreeForData(li, dataNode)
		hierarchyTreeRootDomUl.appendChild(li)
	}
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}

fun renderRoots(error: RootExceptionResult) {
	val messageDom = document.getElementById("message") as HTMLElement
	messageDom.asDynamic().error = error
	messageDom.innerHTML = "<b>${error.ex}</b>: ${error.matcher}"

	val hierarchyDom = document.getElementById("hierarchy-display") as HTMLElement
	while (hierarchyDom.firstChild != null) {
		hierarchyDom.removeChild(hierarchyDom.firstChild!!)
	}

	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	for (root in error.roots) {
		// Create a dynamic object for root
		val rootNode: dynamic = js("{}")
		rootNode.name = root["window-token"]
		rootNode._display = document.createElement("span")

		// Copy all properties from the map
		for ((key, value) in root) {
			rootNode[key] = value
		}

		val children = root["children"] as? List<*> ?: emptyList<Any>()
		for (child in children) {
			if (child is ViewNode) {
				child.props["_display"] = document.createElement("span").toString()
			}
		}

		val li = document.createElement("li") as HTMLLIElement
		li.asDynamic().view = rootNode
		renderTreeForRoot(li, rootNode)
		hierarchyTreeRootDomUl.appendChild(li)
	}
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}

fun renderTreeForData(target: HTMLElement, dataNode: dynamic) {
	target.className = "view"
	val name = document.createElement("span") as HTMLElement
	name.innerText = dataNode.name?.toString() ?: ""
	name.className = "name"
	target.appendChild(name)
	target.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showViewForData(dataNode)
	})
}

fun renderTreeForRoot(target: HTMLElement, root: dynamic) {
	target.className = "view"
	val name = document.createElement("span") as HTMLElement
	name.innerText = root.name?.toString() ?: ""
	name.className = "name"
	target.appendChild(name)
	target.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showViewForRoot(root)
	})

	val children = root.children as? List<*>
	if (children != null && children.isNotEmpty()) {
		val childrenUl = document.createElement("ul") as HTMLUListElement
		for (child in children) {
			if (child is ViewNode) {
				val childLi = document.createElement("li") as HTMLLIElement
				renderTree(childLi, child)
				childrenUl.appendChild(childLi)
			}
		}
		target.appendChild(childrenUl)
	}
}

// Hash code function matching JavaScript implementation
fun String.hashCode32(): Int {
	var hash = 0
	for (element in this) {
		val chr = element.code
		hash = ((hash shl 5) - hash) + chr
		hash = hash or 0 // Convert to 32bit integer
	}
	return hash
}

fun renderView(target: HTMLElement, view: ViewNode, scaleX: Float, scaleY: Float) {
	val dom = document.createElement("div") as HTMLElement
	view.props["_display"] = dom.toString()
	dom.asDynamic().view = view

	dom.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(view)
	})

	val classList = listOfNotNull(
		"view",
		view.name,
		view.props["visibility"],
		if (view.matches) "MATCHES" else null
	)
	dom.className = classList.joinToString(" ")

	val width = view.props["width"]?.toFloatOrNull() ?: 0f
	val height = view.props["height"]?.toFloatOrNull() ?: 0f
	val x = view.props["x"]?.toFloatOrNull() ?: 0f
	val y = view.props["y"]?.toFloatOrNull() ?: 0f

	dom.style.width = "${width * scaleX}%"
	dom.style.height = "${height * scaleY}%"
	dom.style.left = "${x * scaleX}%"
	dom.style.top = "${y * scaleY}%"
	dom.style.backgroundColor = dom.className.hashCode32().toString(16)
	dom.setAttribute("data-type", view.name)

	view.props["text"]?.let { text ->
		dom.innerText = text
	}

	target.appendChild(dom)
	for (child in view.children) {
		renderView(dom, child, 100 / width, 100 / height)
	}
}

fun renderTree(target: HTMLElement, view: ViewNode) {
	view.props["_tree"] = target.toString()
	target.asDynamic().view = view

	target.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(view)
	})

	val classList = listOfNotNull(
		"view",
		view.name,
		view.props["visibility"],
		if (view.matches) "MATCHES" else null
	)
	target.className = classList.joinToString(" ")
	target.setAttribute("data-type", view.name)

	val nameSpan = document.createElement("span") as HTMLElement
	nameSpan.innerText = view.name
	nameSpan.className = "name"
	target.appendChild(nameSpan)

	if (view.children.isNotEmpty()) {
		val childrenUl = document.createElement("ul") as HTMLUListElement
		for (child in view.children) {
			val childLi = document.createElement("li") as HTMLLIElement
			renderTree(childLi, child)
			childrenUl.appendChild(childLi)
		}
		target.appendChild(childrenUl)
	}
}

var lastView: Any? = null

fun showView(view: ViewNode?) {
	// Remove highlight from last view
	if (lastView is ViewNode) {
		val lv = lastView as ViewNode
		lv.props["_display"]?.let { displayRef ->
			document.querySelector("[data-type='${lv.name}']")?.classList?.remove("highlight")
		}
		lv.props["_tree"]?.let { treeRef ->
			document.querySelector("[data-type='${lv.name}']")?.classList?.remove("highlight")
		}
	}

	lastView = view
	if (view == null) return

	// Add highlight to current view
	view.props["_display"]?.let {
		document.querySelector("[data-type='${view.name}']")?.classList?.add("highlight")
	}
	view.props["_tree"]?.let {
		document.querySelector("[data-type='${view.name}']")?.classList?.add("highlight")
	}

	document.getElementById("name")?.textContent = view.name
	document.getElementById("path")?.textContent = "in " + parents(view).joinToString(" > ")

	val props = document.getElementById("properties") as HTMLElement
	while (props.firstChild != null) {
		props.removeChild(props.firstChild!!)
	}

	val exclusions = setOf("children", "parent", "name", "matches", "_display", "_tree")
	val propsToShow = view.props.keys.filter { it !in exclusions }

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

	val sortedProps = propsToShow.sortedWith(compareBy({ getPriority(it) }, { it }))

	for (prop in sortedProps) {
		val propName = document.createElement("dt") as HTMLElement
		val propValue = document.createElement("dd") as HTMLElement
		propName.textContent = prop
		propValue.textContent = view.props[prop]
		props.appendChild(propName)
		props.appendChild(propValue)
	}
}

fun showViewForData(dataNode: dynamic) {
	lastView = dataNode
	document.getElementById("name")?.textContent = dataNode.name?.toString() ?: ""
	document.getElementById("path")?.textContent = ""

	val props = document.getElementById("properties") as HTMLElement
	while (props.firstChild != null) {
		props.removeChild(props.firstChild!!)
	}

	val exclusions = setOf("children", "parent", "name", "matches", "_display", "_tree")
	val propsToShow = mutableListOf<String>()

	// Iterate over dynamic object properties
	for (key in js("Object").keys(dataNode)) {
		val keyStr = key.toString()
		if (keyStr !in exclusions) {
			propsToShow.add(keyStr)
		}
	}

	for (prop in propsToShow.sorted()) {
		val propName = document.createElement("dt") as HTMLElement
		val propValue = document.createElement("dd") as HTMLElement
		propName.textContent = prop
		propValue.textContent = dataNode[prop]?.toString() ?: ""
		props.appendChild(propName)
		props.appendChild(propValue)
	}
}

fun showViewForRoot(root: dynamic) {
	lastView = root
	document.getElementById("name")?.textContent = root.name?.toString() ?: ""
	document.getElementById("path")?.textContent = ""

	val props = document.getElementById("properties") as HTMLElement
	while (props.firstChild != null) {
		props.removeChild(props.firstChild!!)
	}

	val exclusions = setOf("children", "parent", "name", "matches", "_display", "_tree")
	val propsToShow = mutableListOf<String>()

	// Iterate over dynamic object properties
	for (key in js("Object").keys(root)) {
		val keyStr = key.toString()
		if (keyStr !in exclusions) {
			propsToShow.add(keyStr)
		}
	}

	for (prop in propsToShow.sorted()) {
		val propName = document.createElement("dt") as HTMLElement
		val propValue = document.createElement("dd") as HTMLElement
		propName.textContent = prop
		propValue.textContent = root[prop]?.toString() ?: ""
		props.appendChild(propName)
		props.appendChild(propValue)
	}
}

fun parents(view: ViewNode): List<String> {
	val parents = mutableListOf<String>()
	var v = view.parent
	while (v != null) {
		val resName = v.props["res-name"]
		parents.add(if (resName != null) "${v.name} ($resName)" else v.name)
		v = v.parent
	}
	parents.reverse()
	return parents
}
