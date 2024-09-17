@file:DependsOn("org.snakeyaml:snakeyaml-engine:2.8")

import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File

fun main(vararg args: String) {
	val bitriseFile = File(args[0])
	val start = args.drop(1).takeIf { it.isNotEmpty() }
	val load = Load(LoadSettings.builder().build())
	val bitriseYaml = load.loadFromInputStream(bitriseFile.inputStream())
	val bitrise = parse(bitriseYaml)
	val startWorkflows = start?.map { name ->
		bitrise.workflows[name] ?: error("Workflow ${name} doesn't exist in ${bitriseFile}.")
	}
	val mermaid = render(bitrise, startWorkflows)
	println(mermaid)
}

data class Workflow(
	val id: String,
	val title: String,
	val beforeRun: List<String>,
	val afterRun: List<String>,
	val buildRouterStartSteps: List<List<String>>
)

data class BitriseYaml(
	val triggers: Map<String, String>,
	val workflows: Map<String, Workflow>,
)

@Suppress("UNCHECKED_CAST")
fun parse(data: Any): BitriseYaml {
	val bitrise = data as Map<String, Any>

	val triggerMap = bitrise["trigger_map"] as List<Map<String, Any>>
	val triggers = triggerMap.associate {
		val trigger = it.filterNot { it.key == "workflow" }
		val triggeredWorkflow = it["workflow"] as String
		trigger.toString() to triggeredWorkflow
	}

	val workflows = bitrise["workflows"] as Map<String, Map<String, Any>>
	val bitriseWorkflows = workflows.mapValues { (id, workflow) ->
		val title = workflow["title"] as String? ?: ""
		val beforeRun = workflow["before_run"] as List<String>? ?: emptyList()
		val afterRun = workflow["after_run"] as List<String>? ?: emptyList()
		val steps = workflow["steps"] as List<Map<String, Map<String, Any>>>? ?: emptyList()
		val stepsMap = steps.map { it.entries.single() }.associate { it.key to it.value }
		val buildRouterStartSteps = stepsMap
			.filterKeys { name -> name.startsWith("build-router-start@") }
			.mapValues { (_, step) ->
				val inputs = step["inputs"] as List<Map<String, Any>>
				val inputsMap = inputs.reduce(Map<String, Any>::plus)
				(inputsMap["workflows"] as String).lines()
			}
			.map { it.value }
		Workflow(id, title, beforeRun, afterRun, buildRouterStartSteps)
	}
	return BitriseYaml(triggers, bitriseWorkflows)
}

@Suppress("detekt.CyclomaticComplexMethod")
fun render(bitrise: BitriseYaml, start: List<Workflow>?): String = buildString {
	appendLine("%%{init: {'flowchart': {'curve': 'bumpX'}}}%%")
	appendLine("graph LR") // https://mermaid.js.org/syntax/flowchart.html
	appendLine(
		"""
			style trigger_map fill:#FFD700,stroke:#000000,stroke-width:4px,color:black;
			classDef workflow fill:#46B8FF,stroke:#FFFFFF,stroke-width:2px,color:black;
			classDef complexStep fill:#D9D9D9,stroke:#FFFFFF,stroke-width:2px,color:black;
		""".trimIndent()
	)
	appendLine("trigger_map((trigger_map))")

	fun workflow(workflowId: String) {
		if (workflowId.startsWith("_")) {
			appendLine("${workflowId}[${workflowId}]:::complexStep")
		} else {
			appendLine("${workflowId}{{${workflowId}}}:::workflow")
		}
	}

	fun triggers(trigger: String, workflow: String) {
		appendLine("trigger_map -- ${trigger} --> ${workflow}")
	}

	fun calls(caller: String, callee: String) {
		appendLine("${caller} --> ${callee}")
	}

	fun dispatch(caller: String, callee: String) {
		appendLine("${caller} -.-> ${callee}")
	}

	val done = mutableSetOf<String>()
	val queue = (start ?: bitrise.workflows.values).toMutableList()
	while (queue.isNotEmpty()) {
		val workflow = queue.removeFirst()
		if (!done.add(workflow.id)) {
			continue
		}
		workflow(workflow.id)
		workflow.beforeRun.forEach { beforeRun ->
			calls(workflow.id, beforeRun)
			queue.add(bitrise.workflows[beforeRun]!!)
		}
		workflow.afterRun.forEach { afterRun ->
			calls(workflow.id, afterRun)
			queue.add(bitrise.workflows[afterRun]!!)
		}
		workflow.buildRouterStartSteps.forEach { workflows ->
			workflows.forEach { parallelWorkflow ->
				dispatch(workflow.id, parallelWorkflow)
				queue.add(bitrise.workflows[parallelWorkflow]!!)
			}
		}
	}
	bitrise.triggers.forEach { (trigger, workflow) ->
		if (start == null || done.contains(workflow)) {
			triggers(trigger, workflow)
		}
	}
}

@Suppress("detekt.SpreadOperator")
main(*args)
