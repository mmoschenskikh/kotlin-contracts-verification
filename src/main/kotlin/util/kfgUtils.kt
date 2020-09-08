package util

import org.jetbrains.research.kfg.util.Flags
import org.jetbrains.research.kfg.util.KfgClassWriter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter

// Thanks to Azat Abdullin
internal fun ClassNode.recomputeFrames(loader: ClassLoader): ClassNode {
    val ba = this.toByteArray(loader)
    return ba.toClassNode()
}

private fun ByteArray.toClassNode(): ClassNode {
    val classReader = ClassReader(this.inputStream())
    val classNode = ClassNode()
    classReader.accept(classNode, Flags.readAll.value)
    return classNode
}

private fun ClassNode.toByteArray(loader: ClassLoader, flags: Flags = Flags.writeComputeAll): ByteArray {
    val cw = KfgClassWriter(loader, flags)
    val cca = CheckClassAdapter(cw)
    this.accept(cca)
    return cw.toByteArray()
}