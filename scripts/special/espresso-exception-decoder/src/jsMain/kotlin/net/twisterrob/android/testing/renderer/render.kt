package net.twisterrob.android.testing.renderer

import kotlinx.browser.document
import net.twisterrob.android.testing.DataExceptionResult
import net.twisterrob.android.testing.ExceptionResult
import net.twisterrob.android.testing.RootExceptionResult
import net.twisterrob.android.testing.ViewExceptionResult
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLUListElement

internal fun render(error: ExceptionResult) {
	when (error) {
		is ViewExceptionResult -> renderHierarchy(error)
		is DataExceptionResult -> renderData(error)
		is RootExceptionResult -> renderRoots(error)
	}
}

private fun renderRoots(error: RootExceptionResult) {
	val hierarchyTreeDom = document.getElementById("hierarchy-tree") as HTMLElement
	while (hierarchyTreeDom.firstChild != null) {
		hierarchyTreeDom.removeChild(hierarchyTreeDom.firstChild!!)
	}
	val hierarchyTreeRootDomUl = document.createElement("ul") as HTMLUListElement
	for (root in error.roots) {
		root.props["_display"] = document.createElement("span")
		for (child in root.children) {
			child.props["_display"] = document.createElement("span")
		}

		val hierarchyTreeRootDomLi = document.createElement("li") as HTMLLIElement
		renderTree(hierarchyTreeRootDomLi, root)
		hierarchyTreeRootDomUl.appendChild(hierarchyTreeRootDomLi)
	}
	hierarchyTreeDom.appendChild(hierarchyTreeRootDomUl)
}

private fun renderData(error: DataExceptionResult) {
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

private fun renderHierarchy(error: ViewExceptionResult) {
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

private fun String.hashCode32(): Int {
	var hash = 0
	for (element in this) {
		val chr = element.code
		hash = ((hash shl 5) - hash) + chr
		hash = hash or 0 // Convert to 32bit integer
	}
	return hash
}

private fun renderView(target: HTMLElement, view: ViewNode, scaleX: Float, scaleY: Float) {
	val dom = document.createElement("div") as HTMLElement
	view.props["_display"] = dom
	dom.asDynamic().view = view
	dom.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(event.currentTarget.asDynamic().view)
	})
	dom.className =
		listOf("view", view.name, view.props["visibility"], if (view.matches) "MATCHES" else "").joinToString(" ")
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
		renderView(
			dom,
			child,
			100 / (view.props["width"] as String).toFloat(),
			100 / (view.props["height"] as String).toFloat(),
		)
	}
}

private fun renderTree(target: HTMLElement, view: TreeNode) {
	view.props["_tree"] = target
	target.asDynamic().view = view
	target.addEventListener("mouseover", { event ->
		event.stopPropagation()
		showView(event.currentTarget.asDynamic().view)
	})
	target.className = listOf(
		"view",
		view.name,
		view.props["visibility"],
		if (view is ViewNode && view.matches) "MATCHES" else "",
	).joinToString(" ")
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
