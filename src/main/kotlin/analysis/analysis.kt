package analysis

import Copy
import InvertBoolean
import NotNullWhenTrue
import NullWhenTrue
import Rule
import org.jetbrains.research.kfg.InvalidStateError
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Method

fun analyze(method: Method): Map<BasicBlock, Map<String, AnalysisLattice>> {
    val basicBlocks = method.basicBlocks
    val blocksState: MutableMap<BasicBlock, MutableMap<String, AnalysisLattice>> = mutableMapOf()
    val variables = collectMethodVariables(basicBlocks) + collectInstanceOfChecks(basicBlocks)
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
            workList.addAll(basicBlock.predecessors)
        }
    }
    return blocksState
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
    return variables.toSet()
}

private fun collectInstanceOfChecks(basicBlocks: List<BasicBlock>): Set<String> {
    val checks = mutableSetOf<String>()
    basicBlocks.forEach { bb ->
        bb.instructions.forEach { inst ->
            try {
                inst.get()
                val rightSide = inst.print().split(" = ").last()
                if (rightSide.matches(Regex(".* instanceOf .*"))) {
                    checks.add(rightSide)
                }
            } catch (ignored: InvalidStateError) {
            }
        }
    }
    return checks.toSet()
}

private val rules: MutableMap<String, Rule> = mutableMapOf()
private val independent: MutableSet<String> = mutableSetOf()

private fun transform(
    basicBlock: BasicBlock,
    blocksState: MutableMap<BasicBlock, MutableMap<String, AnalysisLattice>>
) {
    val blockName = basicBlock.name.toString()
    blocksState[basicBlock]?.forEach { s, lattice ->
        if (rules.containsKey(s)) {
            val rule = rules[s]!!
            val dependState = blocksState[basicBlock]!![rule.dependingOn]!!.state
            lattice.state = applyRule(rule, lattice.state, dependState)
        } else {
            val predecessorsStates = mutableSetOf<AnalysisLattice.Element>()
            basicBlock.predecessors.forEach {
                predecessorsStates.add(blocksState[it]!![s]!!.state)
            }
            lattice.state = lattice.join(*predecessorsStates.toTypedArray(), lattice.state)
        }
    }
    val assertMatcher = Regex("%assert\\((.*)\\)").toPattern().matcher(blockName)
    val phiMatcher = Regex("%phi\\(([\\da-z%\$]+) = (.+)\\)").toPattern().matcher(blockName)
    when {
        assertMatcher.matches() -> {
            var varName = assertMatcher.group(1)
            val state =
                if (varName.startsWith("!")) {
                    varName = varName.substring(1)
                    AnalysisLattice.Element.FALSE
                } else {
                    AnalysisLattice.Element.TRUE
                }
            blocksState[basicBlock]!![varName]!!.state = state
        }
        phiMatcher.matches() -> {
            val varName = phiMatcher.group(1)
            val rightSide = phiMatcher.group(2)
            blocksState[basicBlock] = modify(varName, rightSide, blocksState[basicBlock]!!) // TODO
        }
        else -> {
            val assignmentPattern = Regex("([\\da-z%\$]+) = (.+)").toPattern()
            basicBlock.instructions.forEach { instruction ->
                try {
                    val assignmentMatcher = assignmentPattern.matcher(instruction.print())

                    val varName = instruction.get().toString()
                    assignmentMatcher.matches()
                    val rightSide = assignmentMatcher.group(2)
                    blocksState[basicBlock] = modify(varName, rightSide, blocksState[basicBlock]!!) // TODO
                } catch (ignored: InvalidStateError) {
                }
            }
        }
    }
}

private fun modify(
    variableName: String,
    rightSide: String,
    blockState: MutableMap<String, AnalysisLattice>
): MutableMap<String, AnalysisLattice> {
    val constants: Set<String> = setOf("null", "1", "0", "static Boolean.valueOf(1)", "static Boolean.valueOf(0)")
    if (rightSide in constants) {
        rules.remove(variableName)
        independent.add(variableName)
    }
    when (rightSide) { // Simple cases; I haven't see pure "true" or "false" here
        "null" -> blockState[variableName]!!.state = AnalysisLattice.Element.NULL

        "1" -> blockState[variableName]!!.state = AnalysisLattice.Element.TRUE
        "static Boolean.valueOf(1)" -> blockState[variableName]!!.state = AnalysisLattice.Element.TRUE

        "0" -> blockState[variableName]!!.state = AnalysisLattice.Element.FALSE
        "static Boolean.valueOf(0)" -> blockState[variableName]!!.state = AnalysisLattice.Element.FALSE

        else -> {
            val concreteValues: Set<AnalysisLattice.Element> = setOf(
                AnalysisLattice.Element.TRUE,
                AnalysisLattice.Element.FALSE,
                AnalysisLattice.Element.OTHER,
                AnalysisLattice.Element.NULL
            )

            val booleanValueOfMatcher = Regex("static Boolean.valueOf\\((.+)\\)").toPattern().matcher(rightSide)
            val comparisonMatcher = Regex("\\((.*) ([!=]=) ([false0nu]+)\\)").toPattern().matcher(rightSide)

            if (rightSide.matches(Regex(".+ instanceOf .+")) && blockState.containsKey(rightSide)) {
                if (!rules.containsKey(rightSide)) {
                    rules[rightSide] = Copy(variableName)
                }
            } else if (booleanValueOfMatcher.matches()) {
                val rightSideVariable = booleanValueOfMatcher.group(1)
                val rightSideVariableState = blockState[rightSideVariable]!!.state
                if (!rules.containsKey(rightSideVariable)) {
                    if (rightSideVariableState in concreteValues) {
                        blockState[variableName]!!.state = rightSideVariableState
                    } else if (rightSideVariable !in independent) {
                        rules[rightSideVariable] = Copy(variableName)
                    }
                }
            } else if (comparisonMatcher.matches()) {
                val operand = comparisonMatcher.group(1)
                val operator = comparisonMatcher.group(2)
                val constant = comparisonMatcher.group(3)

                val operandState = blockState[operand]!!.state
                if (!rules.containsKey(operand)) {
                    if (operandState in concreteValues) {
                        blockState[variableName]!!.state = operandState
                    } else if (operand !in independent) {
                        val falseConstants = setOf("false", "0")
                        rules[operand] = when {
                            operator == "==" && constant in falseConstants -> InvertBoolean(variableName)
                            operator == "!=" && constant in falseConstants -> Copy(variableName)
                            operator == "==" && constant == "null" -> NullWhenTrue(variableName)
                            operator == "!=" && constant == "null" -> NotNullWhenTrue(variableName)
                            else -> throw IllegalStateException("The case is unhandled | $operand $operator $constant")
                        }
                    }
                }
            } else if (rightSide.matches(Regex("%\\d*"))) {
                val state = blockState[rightSide]!!.state
                if (!rules.containsKey(rightSide)) {
                    if (state in concreteValues) {
                        blockState[variableName]!!.state = state
                    } else if (rightSide !in independent) {
                        rules[rightSide] = Copy(variableName)
                    }
                }
            } else {
                blockState[variableName]!!.state = AnalysisLattice.Element.OTHER
            }
        }
    }
    return blockState
//    TODO
}

private fun applyRule(
    rule: Rule,
    state: AnalysisLattice.Element,
    dependState: AnalysisLattice.Element
): AnalysisLattice.Element {
    val booleanValues = setOf(AnalysisLattice.Element.TRUE, AnalysisLattice.Element.FALSE)
    return when (rule) {
        is Copy -> dependState
        is InvertBoolean -> if (dependState in booleanValues) invertBoolean(dependState) else state
        is NullWhenTrue ->
            if (dependState in booleanValues) {
                if (dependState == AnalysisLattice.Element.TRUE)
                    AnalysisLattice.Element.NULL
                else
                    AnalysisLattice.Element.NOTNULL
            } else {
                state
            }
        is NotNullWhenTrue ->
            if (dependState in booleanValues) {
                if (dependState == AnalysisLattice.Element.TRUE)
                    AnalysisLattice.Element.NOTNULL
                else
                    AnalysisLattice.Element.NULL
            } else {
                state
            }
        else -> state
    }
}

private fun invertBoolean(booleanElement: AnalysisLattice.Element): AnalysisLattice.Element =
    when (booleanElement) {
        AnalysisLattice.Element.TRUE -> AnalysisLattice.Element.FALSE
        AnalysisLattice.Element.FALSE -> AnalysisLattice.Element.TRUE
        else -> throw IllegalArgumentException("Cannot invert non-boolean state")
    }
