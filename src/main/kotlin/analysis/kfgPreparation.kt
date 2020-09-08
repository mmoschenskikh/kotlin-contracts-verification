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

    val blocksToAdd = mutableSetOf<BasicBlock>()
    val assertions = mutableMapOf<String, String>()
    val assignments = mutableSetOf<String>()
    val instructionsToDelete = mutableMapOf<Instruction, BasicBlock>()

    // Collecting info about extra blocks needed for analysis
    analyzingMethod.basicBlocks.forEach { bb ->
        bb.instructions.forEach { inst ->
            val stringInst = inst.print()
            if (stringInst.contains("phi")) {
                assignments.add(bb.name.toString())
                instructionsToDelete[inst] = bb
            }
            if (stringInst.startsWith("if")) {
                assertions.putAll(collectIfBranches(inst))
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
                val assignmentOptions = stringInst.dropWhile { it != '{' }.drop(1).dropLast(1).split(";")
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
                val pair =
                    if (blockName.contains("else"))
                        blockName.replace("else", "then")
                    else
                        blockName.replace("then", "else")

                val block = BodyBlock("assert($cond)")
                block.add(InstructionFactory(classManager).getJump(bb))
                // There are some problem
                val condBlock = bb.predecessors.firstOrNull { it.name.toString() != pair } ?: bb.predecessors.first()

                insertBlock(condBlock, block, bb)

                assertions.remove(blockName)
                blocksToAdd.add(block)
            }
        }
    }

    instructionsToDelete.forEach { (inst, bb) -> analyzingMethod.basicBlocks.first { it == bb }.remove(inst) }
    blocksToAdd.forEach { analyzingMethod.basicBlocks.add(it) }
    return analyzingMethod
}

private fun collectIfBranches(instruction: Instruction): Map<String, String> {
    val assertions = mutableMapOf<String, String>()
    val conditionVariable = instruction.operands.first().toString()
    val ifInstruction = instruction.print().split(" ")
    val thenBlock = ifInstruction[ifInstruction.indexOf("goto") + 1]
    val elseBlock = ifInstruction[ifInstruction.indexOf("else") + 1]
    assertions[thenBlock] = conditionVariable
    assertions[elseBlock] = "!$conditionVariable"
    return assertions
}

private fun insertBlock(prev: BasicBlock, inserting: BasicBlock, next: BasicBlock) {
    prev.removeSuccessor(next)
    prev.addSuccessor(inserting)

    inserting.addPredecessor(prev)
    inserting.addSuccessor(next)

    next.removePredecessor(prev)
    next.addPredecessor(inserting)
}
