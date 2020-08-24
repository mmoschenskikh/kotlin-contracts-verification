import kotlinx.metadata.KmEffect
import kotlinx.metadata.KmFunction
import org.objectweb.asm.tree.MethodNode

class KotlinContract private constructor(
    val methodNode: MethodNode,
    val kmFunction: KmFunction,
    val effects: List<KmEffect>
) {
    data class Builder(
        var methodNode: MethodNode? = null,
        var kmFunction: KmFunction? = null,
        var effects: List<KmEffect>? = null
    ) {
        fun methodNode(methodNode: MethodNode) = apply { this.methodNode = methodNode }
        fun kmFunction(kmFunction: KmFunction) = apply { this.kmFunction = kmFunction }
        fun effects(vararg effects: KmEffect) = apply { this.effects = effects.toList() }
        fun build() {
            try {
                KotlinContract(methodNode!!, kmFunction!!, effects!!)
            } catch (npe: NullPointerException) {
                throw IllegalStateException("All the parameters must be specified.")
            }
        }
    }
}