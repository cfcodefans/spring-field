package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Hashtable
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import kotlin.math.pow

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/commander
 * Intent:
 *      Used to handle all problems that can be encountered when doing distributed transactions
 */
object Commander {
    private val log: Logger = LoggerFactory.getLogger(Commander::class.java)

    data class User(var name: String, var address: String)

    open class DatabaseUnavailableException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 2459603L
        }
    }

    open class IsEmptyException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 123546L
        }
    }

    open class ItemUnavailableException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 575940L
        }
    }

    open class PaymentDetailsErrorException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 867203L
        }
    }

    open class ShippingNotPossibleException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 342055L
        }
    }

    abstract class Database<T> {
        @Throws(DatabaseUnavailableException::class)
        abstract fun add(obj: T): T

        @Throws(DatabaseUnavailableException::class)
        abstract fun get(id: String): T?
    }

    val RANDOM: SecureRandom = SecureRandom()
    const val ALL_CHARS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    fun mkRandomStr(): String {
        val random: StringBuilder = StringBuilder()
        while (random.length < 12) {
            random.append(ALL_CHARS[(RANDOM.nextFloat() * ALL_CHARS.length).toInt()])
        }
        return random.toString()
    }

    enum class PaymentStatus {
        NOT_DONE, TRYING, DONE
    }

    enum class MessageSent {
        NONE_SENT, PAYMENT_FAIL, PAYMENT_TRYING, PAYMENT_SUCCESSFUL
    }

    data class Order(val user: User, val item: String, val price: Float) {
        val createdTime: Long = System.currentTimeMillis()

        companion object {
            val USED_IDS: MutableMap<String, Boolean> = hashMapOf()
        }

        lateinit var id: String

        init {
            var id: String = mkRandomStr()
            if (USED_IDS.containsKey(id)) {
                while (USED_IDS[id]!!) {
                    id = mkRandomStr()
                }
            }
            this.id = id
            USED_IDS[id] = true
        }

        var paid: PaymentStatus = PaymentStatus.TRYING
        var messageSent: MessageSent = MessageSent.NONE_SENT
        var addedToEmployeeHandle: Boolean = false
    }

    abstract class Service(protected val database: Database<*>, vararg exs: Exception) {
        open val exceptions: ArrayList<Exception> = arrayListOf(*exs)

        companion object {
            val USED_IDS: Hashtable<String, Boolean> = Hashtable()
            fun generateId(): String {
                var id: String = mkRandomStr()
                if (USED_IDS.contains(id)) {
                    while (USED_IDS[id]!!) {
                        id = generateId()
                    }
                }
                return id
            }
        }

        @Throws(DatabaseUnavailableException::class)
        abstract fun receiveReq(vararg params: Any): String

        @Throws(DatabaseUnavailableException::class)
        abstract fun updateDb(vararg params: Any): String
    }

    fun interface IOperation {
        @Throws(Exception::class)
        fun perform(exs: List<Exception>): Unit
    }

    fun interface IHandleErrorIssue<T> {
        fun handleIssue(obj: T, e: Exception): Unit
    }

    open class Retry<T>(val op: IOperation,
                        val handleError: IHandleErrorIssue<T>,
                        val maxAttempts: Int,
                        val maxDelay: Long,
                        vararg ignoreTests: Predicate<Exception>) {
        val attempts: AtomicInteger = AtomicInteger()
        val test: Predicate<Exception> = ignoreTests.reduce { p1, p2 -> p1.or(p2) }.or { e -> false }
        val errors: MutableList<Exception> = arrayListOf()

        open fun perform(exs: List<Exception>, obj: T): Unit {
            do {
                try {
                    op.perform(exs)
                    return
                } catch (e: Exception) {
                    errors.add(e)
                    if (attempts.incrementAndGet() >= maxAttempts || test.test(e).not()) {
                        handleError.handleIssue(obj, e)
                        return //return here...don't go further
                    }
                    runCatching {
                        Thread.sleep((2
                            .toDouble()
                            .pow(attempts.toDouble()) * 1000L
                                + RANDOM.nextInt(1000))
                            .toLong()
                            .coerceAtMost(maxDelay))
                    }
                }
            } while (true)
        }
    }
}