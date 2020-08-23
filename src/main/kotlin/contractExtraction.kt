import kotlinx.metadata.KmEffectExpression
import kotlinx.metadata.KmEffectType
import kotlinx.metadata.KmFunction
import util.KotlinMetadataExtractor

fun run(bytes: ByteArray) {
    val metadataContainer = KotlinMetadataExtractor.extract(bytes)
    metadataContainer.functions.forEach { extractContractInfo(it) }
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