import analysis.FlatLattice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FlatLatticeTests {

    @Test
    fun joinTest() {
        val signLattice = FlatLattice(setOf('+', '-', '0'))
        val elements = signLattice.elements
        val top = signLattice.top
        val bottom = signLattice.bottom
        elements.forEach {
            assertEquals(it, signLattice.join(it))
        }
        elements.forEach {
            assertEquals(it, signLattice.join(it, it))
        }
        elements.forEach {
            assertEquals(top, signLattice.join(it, top))
        }
        elements.forEach {
            assertEquals(it, signLattice.join(it, bottom))
        }
        elements.forEach {
            val rand = elements.random()
            if (rand != it)
                assertEquals(top, signLattice.join(it, rand))
        }
    }

    @Test
    fun meetTest() {
        val constLattice = FlatLattice(setOf(-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5))
        val elements = constLattice.elements
        val top = constLattice.top
        val bottom = constLattice.bottom
        elements.forEach {
            assertEquals(it, constLattice.meet(it))
        }
        elements.forEach {
            assertEquals(it, constLattice.meet(it, it))
        }
        elements.forEach {
            assertEquals(bottom, constLattice.meet(it, bottom))
        }
        elements.forEach {
            assertEquals(it, constLattice.meet(it, top))
        }
        elements.forEach {
            val rand = elements.random()
            if (rand != it)
                assertEquals(bottom, constLattice.meet(it, rand))
        }
    }

    @Test
    fun stateTest() {
        val lattice = FlatLattice(setOf("2", "3"))
        lattice.state = setOf("2")
        lattice.state = setOf("3")
        lattice.state = setOf("3")
        lattice.state = setOf("2", "3")
        lattice.state = setOf()
        assertFails { lattice.state = setOf("hi ;)") }
    }

}