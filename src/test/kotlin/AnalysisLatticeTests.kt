import analysis.AnalysisLattice
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisLatticeTests {

    @Test
    fun joinTest() {
        val lattice = AnalysisLattice()
        val elements = AnalysisLattice.Element.values()
        val top = AnalysisLattice.Element.TOP
        val bottom = AnalysisLattice.Element.BOTTOM
        elements.forEach {
            assertEquals(it, lattice.join(it))
        }
        elements.forEach {
            assertEquals(it, lattice.join(it, it))
        }
        elements.forEach {
            assertEquals(top, lattice.join(it, top))
        }
        elements.forEach {
            assertEquals(it, lattice.join(it, bottom))
        }
        assertEquals(top, lattice.join(AnalysisLattice.Element.NULL, AnalysisLattice.Element.NOTNULL))
    }
}
