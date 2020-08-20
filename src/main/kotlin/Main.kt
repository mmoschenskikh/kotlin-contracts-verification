import java.io.File

fun main() {
    val bytes = File("target/classes/samples/contracts/ContractsKt.class").readBytes()
    run(bytes)
}