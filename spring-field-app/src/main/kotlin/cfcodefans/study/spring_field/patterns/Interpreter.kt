package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Stack

/**
 * Refers to https://github.com/iluwatar/java-design-patterns/tree/master/interpreter
 * Intent:
 *      Given a language, define a representation for its grammar along with an
 *      interpreter that uses the representation to interpret sentences in the language
 */
object Interpreter {
    private val log: Logger = LoggerFactory.getLogger(Interpreter::class.java)

    abstract class Expr {
        abstract fun interpret(): Int
        abstract override fun toString(): String
    }

    open class NumberExpr(private val number: Int) : Expr() {
        constructor(s: String) : this(s.toInt())

        override fun interpret(): Int = number
        override fun toString(): String = "number"
    }

    open class PlusExpr(private val leftExpr: Expr,
                        private val rightExpr: Expr) : Expr() {
        override fun toString(): String = "+"
        override fun interpret(): Int = leftExpr.interpret() + rightExpr.interpret()
    }

    open class MultiplyExpr(private val leftExpr: Expr,
                            private val rightExpr: Expr) : Expr() {
        override fun toString(): String = "*"
        override fun interpret(): Int = leftExpr.interpret() * rightExpr.interpret()
    }

    open class MinusExpr(private val leftExpr: Expr,
                         private val rightExpr: Expr) : Expr() {
        override fun toString(): String = "-"
        override fun interpret(): Int = leftExpr.interpret() - rightExpr.interpret()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val tokenStr: String = "4 3 2 - 1 + *"
        val stack: Stack<Expr> = Stack()
        for (s in tokenStr.split(" ")) {
            if (s == "+" || s == "-" || s == "*") {
                var rightExpr: Expr = stack.pop()
                var leftExpr: Expr = stack.pop()
                log.info("popped from stack left: $leftExpr right: $rightExpr")

                var operator: Expr = when (s) {
                    "+" -> PlusExpr(leftExpr, rightExpr)
                    "-" -> MinusExpr(leftExpr, rightExpr)
                    else -> MultiplyExpr(leftExpr, rightExpr)
                }
                log.info("operator: $operator")
                val result: Int = operator.interpret()
                val resultExpr: Expr = NumberExpr(result)
                stack.push(resultExpr)
                log.info("push result to stack: $resultExpr")
            } else {
                stack.push(NumberExpr(s).also { log.info("push to stack: $it") })
            }
        }
        log.info("result: ${stack.pop().interpret()}")
    }
}