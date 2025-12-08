package net.twisterrob.android.testing.renderer

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLHeadingElement

private var lastView: TreeNode? = null

internal fun showView(view: TreeNode?) {
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

private fun parents(view: TreeNode): List<String> {
	val parents = mutableListOf<String>()
	var v = view.parent
	while (v != null) {
		val resName = v.props["res-name"]
		parents.add(v.name + (if (resName != null) " (${resName})" else ""))
		v = v.parent
	}
	return parents.reversed()
}
