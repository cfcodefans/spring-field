package cfcodefans.study.spring_field.patterns

import cfcodefans.study.spring_field.patterns.ArrangeActAssert.Cash
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


/**
 * Arrange/Act/Assert (AAA) is a unit test pattern. In this simple example,
 * we have a ({@link Cash}) object for plus, minus and counting amount.
 */
object ArrangeActAssert {

    open class Cash(private var amount: Int) {
        open fun plus(addend: Int): Unit {
            amount += addend
        }

        open fun minus(subtrahend: Int): Boolean = if (amount >= subtrahend) {
            amount -= subtrahend
            true
        } else {
            false
        }

        open fun count(): Int = amount
    }

}

open class CashAAATests {
    @Test
    open fun testPlus() {
        //Arrange
        val cash: Cash = Cash(3)
        //Act
        cash.plus(4)
        //Assert
        assertEquals(7, cash.count())
    }

    @Test
    fun testMinus() {
        //Arrange
        val cash: Cash = Cash(8)
        //Act
        val result = cash.minus(5)
        //Assert
        assertTrue(result)
        assertEquals(3, cash.count())
    }

    @Test
    fun testInsufficientMinus() {
        //Arrange
        val cash: Cash = Cash(1)
        //Act
        val result = cash.minus(6)
        //Assert
        assertFalse(result)
        assertEquals(1, cash.count())
    }

    @Test
    fun testUpdate() {
        //Arrange
        val cash: Cash = Cash(5)
        //Act
        cash.plus(6)
        val result = cash.minus(3)
        //Assert
        assertTrue(result)
        assertEquals(8, cash.count())
    }

    /**
     * ({@link CashAAATest}) is an anti-example of AAA pattern. This test is functionally correct, but
     * with the addition of a new feature, it needs refactoring. There are an awful lot of steps in that
     * test method, but it verifies the class' important behavior in just eleven lines. It violates the
     * single responsibility principle. If this test method failed after a small code change, it might
     * take some digging to discover why.
     */
    @Test
    fun testCash() {
        //initialize
        var cash = Cash(3)
        //test plus
        cash.plus(4)
        assertEquals(7, cash.count())
        //test minus
        cash = Cash(8)
        assertTrue(cash.minus(5))
        assertEquals(3, cash.count())
        assertFalse(cash.minus(6))
        assertEquals(3, cash.count())
        //test update
        cash.plus(5)
        assertTrue(cash.minus(5))
        assertEquals(3, cash.count())
    }
}