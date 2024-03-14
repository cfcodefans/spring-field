package cfcodefans.study.spring_field.patterns

import org.junit.jupiter.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/thread-local-storage
 * Intent:
 *  Provide an ability to have a copy of variable for each thread. making it thread-safe
 */
object ThreadLocalStorage {
    private val log: Logger = LoggerFactory.getLogger(ThreadLocalStorage::class.java)

    private val RND: SecureRandom = SecureRandom()

    private const val RANDOM_THREAD_PARK_START = 1000000000L
    private const val RANDOM_THREAD_PARK_END = 2000000000L

    abstract class AbstractThreadLocalExample : Runnable {

        override fun run() {
            val nanosToPark: Long = RND.nextLong(RANDOM_THREAD_PARK_START, RANDOM_THREAD_PARK_END)
            LockSupport.parkNanos(nanosToPark)
            System.out.println("${Thread.currentThread().name}, before value changing: ${getter()()}")
            setter()(RND.nextLong())
        }

        protected abstract fun getter(): () -> Long
        protected abstract fun setter(): (Long) -> Unit
    }

    open class WithThreadLocal(private val value: ThreadLocal<Long>) : AbstractThreadLocalExample() {
        open fun remove() {
            value.remove()
        }

        override fun setter(): (Long) -> Unit = value::set
        override fun getter(): () -> Long = value::get
    }

    open class WithoutThreadLocal(private var value: Long) : AbstractThreadLocalExample() {
        override fun setter(): (Long) -> Unit = { p -> this.value = p }
        override fun getter(): () -> Long = { this.value }
    }

    val outContent: ByteArrayOutputStream = ByteArrayOutputStream()
    val originalOut: PrintStream = System.out

    @BeforeEach
    open fun setUp() {
        System.setOut(PrintStream(outContent))
    }

    @AfterEach
    open fun reset() {
        System.setOut(originalOut)
    }


    @Test
    @Throws(InterruptedException::class)
    open fun withoutThreadLocal() {
        val initialValue: Long = 1234567890L
        val threadSize: Int = 2
        val executor: ExecutorService = Executors.newFixedThreadPool(threadSize)

        val withoutThreadLocal: WithoutThreadLocal = WithoutThreadLocal(initialValue)
        repeat(2) {
            executor.submit(withoutThreadLocal)
        }
        executor.awaitTermination(3, TimeUnit.SECONDS)
        val logContent: String = outContent.toString()
        log.info("\n" + logContent)
        val initialValueStr: String = initialValue.toString()
        Assertions.assertFalse(logContent.lines().all { line -> line.endsWith(initialValueStr) })
    }

    @Test
    @Throws(InterruptedException::class)
    open fun withThreadLocal() {
        val initialValue: Long = 1234567890L
        val threadSize: Int = 2
        val executor: ExecutorService = Executors.newFixedThreadPool(threadSize)

        val withThreadLocal: WithThreadLocal = WithThreadLocal(ThreadLocal.withInitial {
            initialValue
        })
        repeat(2) {
            executor.submit(withThreadLocal)
        }
        executor.awaitTermination(3, TimeUnit.SECONDS)
        val logContent: String = outContent.toString()
        val initialValueStr: String = initialValue.toString()
        log.info(initialValueStr + "\n" + logContent)
        log.info("\n" + logContent
            .lines()
            .mapIndexed { i, l -> "$i\t$l" }
            .joinToString("\n"))
        Assertions.assertTrue(logContent
            .lines()
            .map { it.trim() }
            .filter { it.isNullOrBlank().not() }
            .all { line -> line.endsWith(initialValueStr) })
    }

}

