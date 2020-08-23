package util

import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

/*
* The most part of code (everything except getting contract information) was stolen from
* https://github.com/udalov/kotlinx-metadata-examples/blob/master/src/main/java/examples/FindKotlinGeneratedMethods.java
* and rewritten in Kotlin
*/

/**
 * Utility for metadata extraction from Kotlin .class files
 */
object KotlinMetadataExtractor {

    /**
     * Gets Kotlin metadata from a .class file.
     */
    fun extract(bytes: ByteArray): KmDeclarationContainer {
        val annotationNode = loadKotlinMetadataAnnotationNode(bytes)
            ?: throw IllegalArgumentException("Not a Kotlin .class file! No @kotlin.Metadata annotation found")
        val header = createHeader(annotationNode)
        return when (val metadata = KotlinClassMetadata.read(header)) {
            is KotlinClassMetadata.Class -> metadata.toKmClass()
            is KotlinClassMetadata.FileFacade -> metadata.toKmPackage()
            is KotlinClassMetadata.MultiFileClassPart -> metadata.toKmPackage()
            else -> throw IllegalArgumentException("Unknown class type")
        }
    }

    /**
     * Loads the AnnotationNode corresponding to the @kotlin.Metadata annotation on the .class file,
     * or returns null if there's no such annotation.
     */
    private fun loadKotlinMetadataAnnotationNode(bytes: ByteArray): AnnotationNode? {
        val node = ClassNode()
        ClassReader(bytes).accept(node, Opcodes.V1_8)
        return node.visibleAnnotations?.firstOrNull { "Lkotlin/Metadata;" == it.desc }
    }

    /**
     * Converts the given AnnotationNode representing the @kotlin.Metadata annotation into KotlinClassHeader,
     * to be able to use it in KotlinClassMetadata.read.
     */
    @Suppress("Unchecked cast")
    private fun createHeader(node: AnnotationNode): KotlinClassHeader {
        var kind: Int? = null
        var metadataVersion: IntArray? = null
        var bytecodeVersion: IntArray? = null
        var data1: Array<String>? = null
        var data2: Array<String>? = null
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int? = null

        val iterator = node.values.iterator()
        while (iterator.hasNext()) {
            val name = iterator.next() as String
            val value = iterator.next()
            when (name) {
                "k" -> kind = value as Int?
                "mv" -> metadataVersion = (value as List<Int>).toIntArray()
                "bv" -> bytecodeVersion = (value as List<Int>).toIntArray()
                "d1" -> data1 = (value as List<String>).toTypedArray()
                "d2" -> data2 = (value as List<String>).toTypedArray()
                "xs" -> extraString = value as String?
                "pn" -> packageName = value as String?
                "xi" -> extraInt = value as Int?
            }
        }
        return KotlinClassHeader(
            kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt
        )
    }
}