package analysis

import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.KfgConfig
import org.jetbrains.research.kfg.builder.cfg.CfgBuilder
import org.jetbrains.research.kfg.ir.BasicBlock
import org.jetbrains.research.kfg.ir.BodyBlock
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.ir.value.instruction.Instruction
import org.jetbrains.research.kfg.ir.value.instruction.InstructionFactory
import org.jetbrains.research.kfg.util.Flags
import org.objectweb.asm.tree.ClassNode
import util.getMethodNodeByName

fun prepareFunctionCfg(classNode: ClassNode, functionName: String): Method {
    val classManager = ClassManager(KfgConfig(Flags.readAll))
    val givenClass = ConcreteClass(classManager, classNode)
    val methodNode = getMethodNodeByName(classNode, functionName)
    val analyzingMethod = Method(classManager, methodNode, givenClass)
    CfgBuilder(classManager, analyzingMethod).build()

    val blocksToAdd: MutableSet<BasicBlock> = mutableSetOf()
    val assertions: MutableMap<String, MutableSet<String>> = mutableMapOf()
    val assignments: MutableSet<String> = mutableSetOf()
    val instructionsToDelete: MutableMap<Instruction, BasicBlock> = mutableMapOf()

    val phiRegex = Regex("[\\da-z%\$]+ = phi \\{(.*)}")

    // Collecting info about extra blocks needed for analysis
    analyzingMethod.basicBlocks.forEach { bb ->
        bb.instructions.forEach { inst ->
            val stringInst = inst.print()
            if (stringInst.matches(phiRegex)) {
                assignments.add(bb.name.toString())
                instructionsToDelete[inst] = bb
            }
            val ifMatcher = Regex("^if \\((.+)\\) goto (.+) else (.+)\$").toPattern().matcher(stringInst)
            if (ifMatcher.matches()) {
                val conditionVariable = ifMatcher.group(1)
                val thenBlock = ifMatcher.group(2)
                val elseBlock = ifMatcher.group(3)
                if (assertions[thenBlock] == null)
                    assertions[thenBlock] = mutableSetOf()
                if (assertions[elseBlock] == null)
                    assertions[elseBlock] = mutableSetOf()
                assertions[thenBlock]!!.add(conditionVariable)
                assertions[elseBlock]!!.add("!$conditionVariable")
                instructionsToDelete[inst] = bb
            }
        }
    }

    // Adding found blocks to the CFG
    analyzingMethod.basicBlocks.forEach { bb ->
        bb.instructions.forEach { inst ->
            val blockName = bb.name.toString()
            if (blockName in assignments) {
                val stringInst = inst.print()
                val assignmentVariable = inst.get()
                val matcher = phiRegex.toPattern().matcher(stringInst)
                check(matcher.matches())
                val assignmentOptions = matcher.group(1).split(";")
                    .map {
                        with(it.trim().split(" -> ")) {
                            this.first() to this.last()
                        }
                    }.toMap()
                assignmentOptions.forEach { (block, value) ->
                    val insertingBlock = BodyBlock("phi($assignmentVariable = $value)")
                    insertingBlock.add(InstructionFactory(classManager).getJump(bb))
                    val prevBlock = bb.predecessors.find { it.name.toString() == block }
                        ?: throw IllegalStateException("Phi node creation failed")

                    insertBlock(prevBlock, insertingBlock, bb)

                    assignments.remove(bb.name.toString())
                    blocksToAdd.add(insertingBlock)
                }
            }
            if (blockName in assertions.keys) {
                val cond = assertions[blockName] ?: throw IllegalStateException("Assert node creation failed")

                cond.forEach { condition ->
                    val block = BodyBlock("assert($condition)")
                    block.add(InstructionFactory(classManager).getJump(bb))

                    val condBlock =
                        bb.predecessors.firstOrNull { predecessor ->
                            predecessor.instructions.any {
                                it.print().matches(Regex("^if \\($condition\\) goto.*else.*$"))
                            }
                        } ?: bb.predecessors.first()
                    insertBlock(condBlock, block, bb)

                    blocksToAdd.add(block)
                }
                assertions.remove(blockName)
            }
        }
    }

    instructionsToDelete.forEach { (inst, bb) -> analyzingMethod.basicBlocks.first { it == bb }.remove(inst) }
    blocksToAdd.forEach { analyzingMethod.basicBlocks.add(it) }
    return analyzingMethod
}

private fun insertBlock(prev: BasicBlock, inserting: BasicBlock, next: BasicBlock) {
    prev.removeSuccessor(next)
    prev.addSuccessor(inserting)

    inserting.addPredecessor(prev)
    inserting.addSuccessor(next)

    next.removePredecessor(prev)
    next.addPredecessor(inserting)
}
