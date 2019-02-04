package de.rhab.wlbtimer

import de.rhab.wlbtimer.model.Break
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun break_rule_calculation_isCorrect() {
        val mBreak = Break()
        assertEquals(-1, mBreak.applyBreakRules(-100))
        assertEquals(-1, mBreak.applyBreakRules(-1))
        assertEquals(0, mBreak.applyBreakRules(0))
        assertEquals(0, mBreak.applyBreakRules(1))
        assertEquals(0, mBreak.applyBreakRules(8998))  // 2:29:58
        assertEquals(0, mBreak.applyBreakRules(8999))  // 2:29:59
        assertEquals(0, mBreak.applyBreakRules(9000))  // 2:30:00
        assertEquals(900, mBreak.applyBreakRules(9001))  // 2:30:01
        assertEquals(900, mBreak.applyBreakRules(9002))  // 2:30:02
        assertEquals(900, mBreak.applyBreakRules(9002))  // 2:30:02
        assertEquals(2700, mBreak.applyBreakRules(35998))  // 9:59:58
        assertEquals(2700, mBreak.applyBreakRules(35999))  // 9:59:59
        assertEquals(2700, mBreak.applyBreakRules(36000))  // 10:00:00
        assertEquals(3600, mBreak.applyBreakRules(36001))  // 10:00:01
    }
}