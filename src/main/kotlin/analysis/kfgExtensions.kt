package analysis

import com.abdullin.kthelper.algorithm.GraphView

fun GraphView.parseLabel(): GraphNode {
    val nodeBuilder = GraphNode.Builder()
    nodeBuilder.graphView = this
    val elements = label.split("\\l").map { it.trim() }.filter { it.isNotBlank() }
    val nameAndPredecessors = elements[0].split(":")
    nodeBuilder.name = nameAndPredecessors.firstOrNull()
    val predecessors = nameAndPredecessors[1].trim().split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    nodeBuilder.predecessors = if (predecessors.isNotEmpty()) predecessors else null
    val gotoString = elements.last()
    var lastInstructionIndex = elements.lastIndex - 1
    when {
        gotoString.startsWith("if") -> {
            val expr = gotoString.split(" ")
            nodeBuilder.successors = setOf(
                expr[expr.indexOf("goto") + 1],
                expr[expr.indexOf("else") + 1]
            )
        }
        gotoString.startsWith("switch") -> {
            val expr = gotoString.split(" ").map {
                it.trim().removePrefix("{").removeSuffix("}").removeSuffix(";")
            }
            nodeBuilder.successors = expr.filter { it == "->" }.map { expr[expr.indexOf(it) + 1] }.toSet()
        }
        gotoString.startsWith("goto") -> {
            nodeBuilder.successors = setOf(gotoString.split(" ").last())
        }
        else -> lastInstructionIndex += 1
    }
    val instructions = mutableListOf<String>()
    for (i in 1..lastInstructionIndex) {
        instructions.add(elements[i])
    }
    nodeBuilder.instructions = if (instructions.isNotEmpty()) instructions.toList() else null
    return nodeBuilder.build()
}