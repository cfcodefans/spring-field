package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/throttling
 * Intent:
 *  Ensure that a given client
 */
object Throttling {
    private val log: Logger = LoggerFactory.getLogger(Throttling::class.java)

    fun interface IThrottier {
        fun start()
    }

    class CallsCount {
        private val tenantCallsCount: MutableMap<String, AtomicLong> = ConcurrentHashMap()

        fun addTenant(tenantName: String): CallsCount = apply {
            tenantCallsCount.putIfAbsent(tenantName, AtomicLong(0))
        }

        fun incrementCount(tenantName: String): Long = tenantCallsCount[tenantName]!!.incrementAndGet()

        fun getCount(tenantName: String): Long = tenantCallsCount[tenantName]!!.get()

        fun reset(): CallsCount = apply {
            tenantCallsCount.replaceAll { k, v -> AtomicLong(0) }
            log.info("reset counters: ${tenantCallsCount.size}")
        }
    }

    open class ThrottleTimerImpl(private val throttlePeriod: Int,
                                 private val callsCount: CallsCount) : IThrottier {
        override fun start() {
            Timer(true).schedule(object : TimerTask() {
                override fun run() {
                    callsCount.reset()
                }
            }, 0L, throttlePeriod.toLong())
        }
    }

    class BarCustomer(val name: String, val allowedCallsPerSecond: Int, callsCount: CallsCount) {
        init {
            require(allowedCallsPerSecond > 0) { "invalid number of calls: $allowedCallsPerSecond" }
            callsCount.addTenant(name)
        }
    }

    class Bartender(private val callsCount: CallsCount, timer: IThrottier) {
        init {
            timer.start()
        }

        open fun orderDrink(barCustomer: BarCustomer): Int {
            val tenantName: String = barCustomer.name
            val count: Long = callsCount.getCount(tenantName)
            if (count >= barCustomer.allowedCallsPerSecond) {
                log.error("I'am sorry $tenantName, you've had $count drinks for today")
                return -1
            }

            callsCount.incrementCount(tenantName)
            log.info("serving beer to $tenantName: [${count + 1}] consumed")
            return ThreadLocalRandom.current().nextInt(1, 10000)
        }
    }

    fun mkServiceCalls(barCustomer: BarCustomer, callsCount: CallsCount) {
        val timer: IThrottier = ThrottleTimerImpl(1000, callsCount)
        var service: Bartender = Bartender(callsCount = callsCount, timer = timer)

        for (i in 0..50) {
            service.orderDrink(barCustomer)
            kotlin.runCatching { Thread.sleep(100) }
                .onFailure { log.error("Thread interrupted:", it) }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val callsCount: CallsCount = CallsCount()
        val human: BarCustomer = BarCustomer(name = "young human", 2, callsCount)
        val dwarf: BarCustomer = BarCustomer(name = "dwarf soldier", 4, callsCount)

        val executor: ExecutorService = Executors.newFixedThreadPool(2)

        executor.submit { mkServiceCalls(human, callsCount) }
        executor.submit { mkServiceCalls(dwarf, callsCount) }

        executor.shutdown()
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}