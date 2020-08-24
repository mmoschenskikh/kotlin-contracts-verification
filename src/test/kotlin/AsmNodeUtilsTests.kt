import util.getClassNode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AsmNodeUtilsTests {

    @Test
    fun javaClassTest() {
        val fields = arrayOf("word", "number")
        val bytes = File("target/test-classes/samples/JavaSampleClass.class").readBytes()
        val classNode = getClassNode(bytes)
        assertTrue(classNode.name.contains("JavaSampleClass"))
        assertTrue(classNode.methods.any { it.name == "doMagic" })
        val classNodeFields = classNode.fields.map { it.name }
        assertTrue(fields.all { it in classNodeFields })
    }

    @Test
    fun kotlinClassTest() {
        val fields = arrayOf("age", "firstName", "secondName", "birthDate", "birthPlace", "occupation")
        val bytes = File("target/test-classes/samples/Person.class").readBytes()
        val classNode = getClassNode(bytes)
        assertTrue(classNode.name.contains("Person"))
        val classNodeFields = classNode.fields.map { it.name }
        assertTrue(fields.all { it in classNodeFields })
    }

    @Test
    fun kotlinFileTest() {
        val names = arrayOf("multipleEffects", "contractOnReceiver", "returnsTrueContract", "returnsFalseContract")
        val bytes = File("target/test-classes/samples/MyContractsKt.class").readBytes()
        val classNode = getClassNode(bytes)
        assertTrue(classNode.name.contains("MyContractsKt"))
        assertTrue(classNode.methods.all { it.name in names })
    }
}