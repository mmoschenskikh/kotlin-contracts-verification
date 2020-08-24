package util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

fun getClassNode(bytes: ByteArray): ClassNode {
    val node = ClassNode()
    ClassReader(bytes).accept(node, Opcodes.V1_8)
    return node
}

fun getMethodNodeByName(classNode: ClassNode, name: String) =
    classNode.methods.find { it.name == name } ?: throw NoSuchElementException("Method $name was not found.")

fun getMethodNodeByName(bytes: ByteArray, name: String) =
    getMethodNodeByName(getClassNode(bytes), name)