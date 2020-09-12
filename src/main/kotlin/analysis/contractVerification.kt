package analysis

import com.abdullin.kthelper.collection.dequeOf
import org.jetbrains.research.kfg.ir.BasicBlock

fun checkReturnsContract(
    blockStates: Map<BasicBlock, Map<String, AnalysisLattice>>,
    conditions: Map<String, AnalysisLattice.Element>
): ContractInfo {
    val returnBlock = blockStates.keys.find { bb -> bb.instructions.any { it.print().startsWith("return") } }
        ?: throw IllegalStateException("No return block found")
    return checkConditions(blockStates[returnBlock]!!, conditions)
}

fun checkReturnsNotNullContract(
    blockStates: Map<BasicBlock, Map<String, AnalysisLattice>>,
    conditions: Map<String, AnalysisLattice.Element>
): ContractInfo {
    val returnBlock = blockStates.keys.find { bb -> bb.instructions.any { it.print().startsWith("return") } }
        ?: throw IllegalStateException("No return block found")
    val returnValue =
        try {
            returnBlock.instructions.last().operands.first().toString()
        } catch (e: NoSuchElementException) {
            "Unit"
        }

    when (returnValue) {
        "null" -> return ContractInfo.INAPPLICABLE_CONTRACT
        else ->
            if (listOf(Regex("%\\d+"), Regex("arg\\$\\d+")).none { returnValue.matches(it) })
                return checkConditions(blockStates[returnBlock]!!, conditions)
    }

    when {
        blockStates[returnBlock]!![returnValue]!!.state == AnalysisLattice.Element.BOTTOM ->
            /*throw UnsupportedOperationException("No information gained")*/
            return ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT
        blockStates[returnBlock]!![returnValue]!!.state == AnalysisLattice.Element.NULL ->
            return ContractInfo.INAPPLICABLE_CONTRACT
        blockStates[returnBlock]!![returnValue]!!.state != AnalysisLattice.Element.TOP ->
            return checkConditions(blockStates[returnBlock]!!, conditions)
    }

    val blocks = dequeOf(returnBlock)
    var block = blocks.pop()
    val highestState = blockStates[block]!![returnValue]!!.state

    while (true) {
        blocks.addAll(block.predecessors)
        if (block.predecessors.any { blockStates[it]!![returnValue]!!.state != highestState })
            break
        block = blocks.pop()
    }

    for (b in blocks) {
        if (blockStates[b]!![returnValue]!!.state != AnalysisLattice.Element.NULL)
            if (checkConditions(
                    blockStates[b]!!,
                    conditions
                ) == ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT
            )
                return ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT
    }
    return ContractInfo.FUNCTION_MATCHES_THE_CONTRACT
}

private val notNullValues = setOf(
    AnalysisLattice.Element.TRUE,
    AnalysisLattice.Element.FALSE,
    AnalysisLattice.Element.OTHER,
    AnalysisLattice.Element.NOTNULL
)

private fun checkConditions(
    blockState: Map<String, AnalysisLattice>,
    conditions: Map<String, AnalysisLattice.Element>
): ContractInfo =
    if (conditions.all {
            with((blockState[it.key] ?: return ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT).state) {
                this == it.value || (it.value == AnalysisLattice.Element.NOTNULL && this in notNullValues)
            }
        })
        ContractInfo.FUNCTION_MATCHES_THE_CONTRACT
    else
        ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT
