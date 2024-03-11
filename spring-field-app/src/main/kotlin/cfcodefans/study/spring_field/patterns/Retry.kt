package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serial
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import kotlin.math.pow


/**
 * refers to https://github.com/iluwatar/java-design-patterns/blob/master/retry/README.md
 * Intent:
 *  Transparently retry certain operations that involve communication with external resources,
 *  Particularly over the network, isolating calling code from the retry implementation details.
 */
object Retry {
    private val log: Logger = LoggerFactory.getLogger(Retry::class.java)

    open class BizException(msg: String) : Exception(msg) {
        companion object {
            @Serial
            private val serialVersionUID: Long = 6235833142062144336L
        }
    }

    open class CustomerNotFoundException(msg: String) : BizException(msg = msg) {
        companion object {
            @Serial
            private val serialVersionUID: Long = -6972888602621778664L
        }
    }

    open class DatabaseNotAvailableException(msg: String) : BizException(msg = msg) {
        companion object {
            @Serial
            private val serialVersionUID: Long = -3750769625095997799L
        }
    }

    data class FindCustomer(var customerId: String,
                            var errors: Deque<BizException>) : IBizOper<String> {
        constructor(customerId: String,
                    vararg errs: BizException)
                : this(customerId = customerId,
                errors = ArrayDeque(listOf(*errs)))

        @Throws(BizException::class)
        override fun perform(): String {
            if (this.errors.isNotEmpty()) throw this.errors.pop()
            return this.customerId
        }
    }

    private val RANDOM: Random = Random()

    open class BasicRetry<T>(protected val op: IBizOper<T>,
                             protected val maxAttempts: Int,
                             protected val maxDelay: Long,
                             vararg ignoreTests: Predicate<Exception>) : IBizOper<T> {
        protected val test: Predicate<Exception> = ignoreTests.reduce(Predicate<Exception>::or)
        protected val attempts: AtomicInteger = AtomicInteger()
        protected val errors: MutableList<Exception> = arrayListOf()

        fun errors(): List<java.lang.Exception> = Collections.unmodifiableList(this.errors)

        fun attempts(): Int = attempts.toInt()

        @Throws(BizException::class)
        override fun perform(): T {
            do {
                try {
                    return op.perform()
                } catch (e: BizException) {
                    errors.add(e)

                    if (attempts.incrementAndGet() >= maxAttempts || test.test(e).not()) throw e

                    kotlin.runCatching {
                        Thread.sleep(maxDelay)
                    }
                }
            } while (true)
        }
    }

    class RetryExponentialBackoff<T>(op: IBizOper<T>,
                                     maxAttempts: Int,
                                     maxDelay: Long,
                                     vararg ignoreTests: Predicate<Exception>) : BasicRetry<T>(op, maxAttempts, maxDelay, *ignoreTests) {

        @Throws(BizException::class)
        override fun perform(): T {
            do {
                try {
                    return op.perform()
                } catch (e: BizException) {
                    errors.add(e)

                    if (attempts.incrementAndGet() >= maxAttempts || test.test(e).not()) throw e

                    kotlin.runCatching {
                        (2.0.pow(this.attempts().toDouble()) * 1000L + RANDOM.nextInt(1000))
                            .coerceAtLeast(maxDelay.toDouble())
                            .let { Thread.sleep(it.toLong()) }
                    }
                }
            } while (true)
        }
    }

    fun interface IBizOper<T> {
        @Throws(BizException::class)
        fun perform(): T
    }

    const val NOT_FOUND: String = "not found"

    private var op: IBizOper<String>? = null

    @Throws(Exception::class)
    fun noErrors() {
        op = FindCustomer("123")
        op!!.perform()
        log.info("Sometimes the operation executes with no errors.")
    }

    @Throws(Exception::class)
    private fun errorNoRetry() {
        op = FindCustomer("123", CustomerNotFoundException(NOT_FOUND))
        try {
            op!!.perform()
        } catch (e: CustomerNotFoundException) {
            log.info("Yet the operation will throw an error every once in a while.")
        }
    }

    @Throws(java.lang.Exception::class)
    private fun errorWithRetry() {
        op = BasicRetry<String>(op = FindCustomer("123", CustomerNotFoundException(NOT_FOUND)),
                maxAttempts = 3,  //3 attempts
                maxDelay = 100, //100 ms delay between attempts
                { e -> e is CustomerNotFoundException })
        val customerId = op!!.perform()
        log.info("However, retrying the operation while ignoring a recoverable error will eventually yield the result $customerId " +
                "after a number of attempts ${(op as BasicRetry<*>).attempts()}")
    }

    @Throws(java.lang.Exception::class)
    private fun errorWithRetryExponentialBackoff() {
        op = RetryExponentialBackoff<String>(op = FindCustomer("123", CustomerNotFoundException(NOT_FOUND)),
                maxAttempts = 3,  //3 attempts
                maxDelay = 100, //100 ms delay between attempts
                { e -> e is CustomerNotFoundException })
        val customerId = op!!.perform()
        log.info("However, retrying the operation while ignoring a recoverable error will eventually yield the result $customerId " +
                "after a number of attempts ${(op as BasicRetry<*>).attempts()}")
    }

    @Throws(java.lang.Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        noErrors()
        errorNoRetry()
        errorWithRetry()
        errorWithRetryExponentialBackoff()
    }
}