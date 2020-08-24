package analysis

import com.abdullin.kthelper.algorithm.GraphView

fun GraphView.parseLabel(): GraphNode {
    val nodeBuilder = GraphNode.Builder()
    nodeBuilder.graphView = this
    nodeBuilder.name = this.name

    val elements = label.split("\\l").map { it.trim() }.filter { it.isNotBlank() }

    val predecessors = elements[0]
        .split(":")[1]
        .trim()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()
    nodeBuilder.predecessors = predecessors.thisOrNull()

    val successors = this.successors.map { it.name }.toSet()
    nodeBuilder.successors = successors.thisOrNull()

    val instructions = mutableListOf<String>()
    for (i in 1..elements.lastIndex) {
        instructions.add(elements[i])
    }
    nodeBuilder.instructions = instructions.thisOrNull()
    return nodeBuilder.build()
}

/**
 * Returns null if set is empty or set itself otherwise.
 */
private fun <T> Set<T>.thisOrNull(): Set<T>? = if (this.isEmpty()) null else this

/**
 * Returns null if list is empty or list itself otherwise.
 */
private fun <T> List<T>.thisOrNull(): List<T>? = if (this.isEmpty()) null else this