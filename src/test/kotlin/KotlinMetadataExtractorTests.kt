import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmPackage
import util.KotlinMetadataExtractor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KotlinMetadataExtractorTests {

    @Test
    fun javaClassTest() {
        val bytes = File("target/test-classes/samples/JavaSampleClass.class").readBytes()
        assertFails { KotlinMetadataExtractor.extract(bytes) }
    }

    @Test
    fun javaClassWithAnnotationTest() {
        val bytes = File("target/test-classes/samples/JavaSampleClassWithAnnotation.class").readBytes()
        assertFailsWith(IllegalArgumentException::class) { KotlinMetadataExtractor.extract(bytes) }
    }

    @Test
    fun kotlinFileTest() {
        val names = arrayOf("multipleEffects", "contractOnReceiver", "returnsTrueContract", "returnsFalseContract")
        val bytes = File("target/test-classes/samples/MyContractsKt.class").readBytes()
        val container = KotlinMetadataExtractor.extract(bytes)
        assertTrue(container is KmPackage)
        assertTrue(container.functions.all { it.name in names })
    }

    @Test
    fun kotlinClassTest() {
        val bytes = File("target/test-classes/samples/Person.class").readBytes()
        val container = KotlinMetadataExtractor.extract(bytes)
        assertTrue(container is KmClass)
        assertTrue(container.name.contains("Person"))
        assertTrue(Flag.Class.IS_DATA.invoke(container.flags))
    }
}