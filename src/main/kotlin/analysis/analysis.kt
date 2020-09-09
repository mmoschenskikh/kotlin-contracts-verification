package analysis

import org.jetbrains.research.kfg.InvalidStateError
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Method

fun analyze(method: Method) {
    val basicBlocks = method.basicBlocks
    val blocksState: MutableMap<BasicBlock, MutableMap<String, AnalysisLattice>> = mutableMapOf()
    val variables = collectMethodVariables(basicBlocks)
    basicBlocks.forEach { bb ->
        blocksState[bb] = variables.map { it to AnalysisLattice() }.toMap().toMutableMap()
    }
    // A work-list fixed-point algorithm
    val workList = basicBlocks.toMutableList()
    while (workList.isNotEmpty()) {
        // must be faster than just random pick because using ArrayList
        val basicBlock = workList.last()
        workList.removeLast()

        val state: MutableMap<String, AnalysisLattice> = mutableMapOf()
        blocksState[basicBlock]!!.forEach { (variable, analysisLattice) ->
            state[variable] = AnalysisLattice()
            state[variable]!!.state = analysisLattice.state
        }
        transform(basicBlock, blocksState)
        if (state != blocksState[basicBlock]) {
            workList.addAll(basicBlock.successors)
        }
    }
}

private fun collectMethodVariables(basicBlocks: List<BasicBlock>): Set<String> {
    val variables = mutableSetOf<String>()
    basicBlocks.forEach { bb ->
        bb.instructions.forEach { inst ->
            variables.addAll(inst.operands
                .map { it.toString() }
                .filter { op -> listOf("arg", "%").any { op.startsWith(it) } })
            try {
                variables.add(inst.get().toString())
            } catch (ignored: InvalidStateError) {
            }
        }
    }
    return variables
}

private fun transform(basicBlock: BasicBlock, state: MutableMap<BasicBlock, MutableMap<String, AnalysisLattice>>) {}
