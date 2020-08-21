import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmEffectExpression
import kotlinx.metadata.KmEffectType
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

/* The most part of code (everything except getting contract information) was stolen from
* https://github.com/udalov/kotlinx-metadata-examples/blob/master/src/main/java/examples/FindKotlinGeneratedMethods.java
* and rewritten in Kotlin
*/

/**
 * @param bytes array containing the bytes of the .class file
 */
fun run(bytes: ByteArray) {
    val annotationNode = loadKotlinMetadataAnnotationNode(bytes)
            ?: throw IllegalArgumentException("Not a Kotlin .class file! No @kotlin.Metadata annotation found")
    val header = createHeader(annotationNode)
    val metadata = KotlinClassMetadata.read(header)

    val container: KmDeclarationContainer
    container = when (metadata) {
        is KotlinClassMetadata.Class -> metadata.toKmClass()
        is KotlinClassMetadata.FileFacade -> metadata.toKmPackage()
        is KotlinClassMetadata.MultiFileClassPart -> metadata.toKmPackage()
        else -> return
    }

    container.functions.forEach { extractContractInfo(it) }
}

private fun extractContractInfo(kmFunction: KmFunction) {
    println(
            "*** fun " + kmFunction.name +
                    "(" + kmFunction.valueParameters.joinToString(separator = ", ") { it.name } + ")"
    )
    kmFunction.contract?.effects?.forEach { kmEffect ->
        val effectType = kmEffect.type
        print(effectType)
        if (effectType == KmEffectType.CALLS) {
            val invocationKind = kmEffect.invocationKind ?: "UNKNOWN"
            print(" $invocationKind")
            // Index of lambda-parameter (0 means receiver -> starts with 1)
            val lambdaIndex = kmEffect.constructorArguments[0].parameterIndex!!
            // Hope lambda can't be a function receiver
            println(", lambda name: " + kmFunction.valueParameters[lambdaIndex - 1].name)
            return
        } else {
            kmEffect.constructorArguments.forEach { print(" " + it.constantValue!!.component1()) }
        }
        print(" -> ")
        require(kmEffect.conclusion != null) // Contracts must have conditional effects
        println(parseExpression(kmFunction, kmEffect.conclusion!!))
    } ?: return
}

private fun parseExpression(function: KmFunction, expression: KmEffectExpression): String {
    val andArgsCount = expression.andArguments.size
    val orArgsCount = expression.orArguments.size

    val result = StringBuilder()

    val paramIndex = expression.parameterIndex
    if (paramIndex != null) {
        val paramName =
                if (paramIndex == 0) "this@${function.name}"
                else function.valueParameters[paramIndex - 1].name
        val instanceCheck = expression.isInstanceType
        val flags = expression.flags
        when {
            instanceCheck != null -> {
                val isWord = if (flags == 0) "is" else "!is"
                result.append("$paramName $isWord ${instanceCheck.classifier}")
            }
            flags != 0 -> {
                val constValue = expression.constantValue
                when (flags) {
                    1 -> result.append("!").append(paramName)
                    2 -> result.append(paramName).append(" == ").append(constValue)
                    3 -> result.append(paramName).append(" != ").append(constValue)
                    else -> throw IllegalStateException("Unknown flag: $flags")
                }
            }
            else -> {
                result.append(paramName)
            }
        }
    }

    if (andArgsCount != 0) {
        expression.andArguments.forEach {
            result.append(" && ").append(parseExpression(function, it))
        }
    }
    if (orArgsCount != 0) {
        expression.orArguments.forEach {
            result.append(" || ").append(parseExpression(function, it))
        }
    }

    if (result.startsWith(" || ") || result.startsWith(" && "))
        result.delete(0, 4)

    return result.toString()
}


/**
 * Loads the AnnotationNode corresponding to the @kotlin.Metadata annotation on the .class file,
 * or returns null if there's no such annotation.
 */
private fun loadKotlinMetadataAnnotationNode(bytes: ByteArray): AnnotationNode? {
    val node = ClassNode()
    ClassReader(bytes).accept(node, Opcodes.V1_8)
    return node.visibleAnnotations.firstOrNull { "Lkotlin/Metadata;" == it.desc }
}


/**
 * Converts the given AnnotationNode representing the @kotlin.Metadata annotation into KotlinClassHeader,
 * to be able to use it in KotlinClassMetadata.read.
 */
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
