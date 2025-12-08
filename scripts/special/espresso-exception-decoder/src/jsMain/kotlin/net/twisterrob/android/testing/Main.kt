package net.twisterrob.android.testing

import kotlinx.browser.document
import kotlinx.browser.window
import net.twisterrob.android.testing.parser.parse
import net.twisterrob.android.testing.renderer.render
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement

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
