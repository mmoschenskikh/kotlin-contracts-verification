package analysis

import org.jetbrains.research.kfg.InvalidStateError
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.Method

class Analyzer {
    private val rules: MutableMap<String, Rule> = mutableMapOf()
    private val independent: MutableSet<String> = mutableSetOf()

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

    private fun transform(
        basicBlock: BasicBlock,
        blocksState: MutableMap<BasicBlock, MutableMap<String, AnalysisLattice>>
    ) {
        val blockName = basicBlock.name.toString()
        blocksState[basicBlock]?.forEach { s, lattice ->
            val predecessorsStates = mutableSetOf<AnalysisLattice.Element>()
            basicBlock.predecessors.forEach {
                predecessorsStates.add(blocksState[it]!![s]!!.state)
            }
            if (rules.containsKey(s)) {
                val rule = rules[s]!!
                val dependState = blocksState[basicBlock]!![rule.dependingOn]!!.state
                lattice.state = applyRule(rule, dependState) ?: lattice.join(
                    *predecessorsStates.toTypedArray(),
                    lattice.state
                )

            } else {
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
                blocksState[basicBlock] = modify(varName, rightSide, blocksState[basicBlock]!!)
            }
            else -> {
                val assignmentPattern = Regex("([\\da-z%\$]+) = (.+)").toPattern()
                basicBlock.instructions.forEach { instruction ->
                    try {
                        val assignmentMatcher = assignmentPattern.matcher(instruction.print())

                        val varName = instruction.get().toString()
                        assignmentMatcher.matches()
                        val rightSide = assignmentMatcher.group(2)
                        blocksState[basicBlock] = modify(varName, rightSide, blocksState[basicBlock]!!)
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
                val staticBooleanValueMatcher =
                    Regex("virtual (.+)\\.booleanValue\\(\\)").toPattern().matcher(rightSide)
                val booleanCastMatcher = Regex("\\(java/lang/Boolean\\) (.+)").toPattern().matcher(rightSide)
                val comparisonMatcher = Regex("\\((.*) ([!=]=) ([false0nu]+)\\)").toPattern().matcher(rightSide)

                if (rightSide.matches(Regex(".+ instanceOf .+")) && blockState.containsKey(rightSide)) {
                    if (!rules.containsKey(rightSide)) {
                        rules[rightSide] = Rule(variableName, Rule.Type.COPY)
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
                                operator == "==" && constant in falseConstants -> Rule(
                                    variableName,
                                    Rule.Type.INVERT_BOOLEAN
                                )
                                operator == "!=" && constant in falseConstants -> Rule(variableName, Rule.Type.COPY)
                                operator == "==" && constant == "null" -> Rule(variableName, Rule.Type.NULL_WHEN_TRUE)
                                operator == "!=" && constant == "null" -> Rule(
                                    variableName,
                                    Rule.Type.NOT_NULL_WHEN_TRUE
                                )
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
                            rules[rightSide] = Rule(variableName, Rule.Type.COPY)
                        }
                    }
                } else {
                    val rightSideVariable = when {
                        staticBooleanValueMatcher.matches() -> staticBooleanValueMatcher.group(1)
                        booleanCastMatcher.matches() -> booleanCastMatcher.group(1)
                        booleanValueOfMatcher.matches() -> booleanValueOfMatcher.group(1)
                        else -> null
                    }
                    if (rightSideVariable != null) {
                        val rightSideVariableState = blockState[rightSideVariable]!!.state

                        if (!rules.containsKey(rightSideVariable)) {
                            if (rightSideVariableState in concreteValues) {
                                blockState[variableName]!!.state = rightSideVariableState
                            } else if (rightSideVariable !in independent) {
                                rules[rightSideVariable] = Rule(variableName, Rule.Type.COPY)
                            }
                        }
                    } else {
                        blockState[variableName]!!.state = AnalysisLattice.Element.OTHER
                        independent.add(variableName)
                        rules.remove(variableName)
                    }
                }
            }
        }
        return blockState
    }

    private fun applyRule(
        rule: Rule,
        dependState: AnalysisLattice.Element
    ): AnalysisLattice.Element? {
        val booleanValues = setOf(AnalysisLattice.Element.TRUE, AnalysisLattice.Element.FALSE)
        return when (rule.ruleType) {
            Rule.Type.COPY ->
                dependState
            Rule.Type.INVERT_BOOLEAN ->
                if (dependState in booleanValues)
                    invertBoolean(dependState)
                else
                    null
            Rule.Type.NULL_WHEN_TRUE ->
                if (dependState in booleanValues) {
                    if (dependState == AnalysisLattice.Element.TRUE)
                        AnalysisLattice.Element.NULL
                    else
                        AnalysisLattice.Element.NOTNULL
                } else {
                    null
                }
            Rule.Type.NOT_NULL_WHEN_TRUE ->
                if (dependState in booleanValues) {
                    if (dependState == AnalysisLattice.Element.TRUE)
                        AnalysisLattice.Element.NOTNULL
                    else
                        AnalysisLattice.Element.NULL
                } else {
                    null
                }
        }
    }

    private fun invertBoolean(booleanElement: AnalysisLattice.Element): AnalysisLattice.Element =
        when (booleanElement) {
            AnalysisLattice.Element.TRUE -> AnalysisLattice.Element.FALSE
            AnalysisLattice.Element.FALSE -> AnalysisLattice.Element.TRUE
            else -> throw IllegalArgumentException("Cannot invert non-boolean state")
        }
}

