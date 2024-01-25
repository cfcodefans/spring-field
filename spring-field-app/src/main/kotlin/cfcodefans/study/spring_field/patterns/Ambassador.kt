package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import kotlin.math.floor
import kotlin.system.measureTimeMillis

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/ambassador
 */
object Ambassador {
    private val log: Logger = LoggerFactory.getLogger(Ambassador::class.java)

    /**
     * Interface shared by ([RemoteService]) and ([ServiceAmbassador]).
     */
    internal interface IRemoteService {
        fun doRemoteFunction(value: Int): Long
    }

    private const val THRESHOLD: Int = 200

    open class RemoteService(private val randomProvider: IRandomProvider = Math::random) : IRemoteService {
        /**
         * Remote function takes a value and multiplies it by 10 taking a random amount of time. Will
         * sometimes return -1. This imitates connectivity issues a client might have to account for.
         *
         * @param value integer value to be multiplied.
         * @return if waitTime is less than {@link RemoteService#THRESHOLD}, it returns value * 10,
         *     otherwise {@link RemoteServiceStatus#FAILURE}.
         */
        override fun doRemoteFunction(value: Int): Long {
            val waitTime: Long = floor(randomProvider() * 1000).toLong()
            try {
                sleep(waitTime)
            } catch (e: InterruptedException) {
                log.error("Thread sleep state interrupted", e);
                Thread.currentThread().interrupt();
            }
            return if (waitTime < THRESHOLD)
                (value * 10).toLong()
            else
                RemoteServiceStatus.FAILURE.remoteServiceStatusValue
        }
    }

    enum class RemoteServiceStatus(val remoteServiceStatusValue: Long) {
        FAILURE(-1)
    }

    /**
     * refers to https://www.baeldung.com/kotlin/lazy-initialization
     */
    private val service: IRemoteService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RemoteService() }

    /**
     * ServiceAmbassador provides an interface for a ({@link Client}) to access ({@link RemoteService}).
     * The interface adds logging, latency testing and usage of the service in a safe way that will not
     * add stress to the remote service when connectivity issues occur.
     */
    const val RETRIES: Int = 3
    const val DELAY_MS: Long = 3000

    open class ServiceAmbassador : IRemoteService {
        override fun doRemoteFunction(value: Int): Long = safeCall(value)

        private fun safeCall(value: Int): Long {
            var retries: Int = 0
            var result: Long = RemoteServiceStatus.FAILURE.remoteServiceStatusValue
            var i: Int = 0; while (i < RETRIES) {
                if (retries >= RETRIES) return RemoteServiceStatus.FAILURE.remoteServiceStatusValue

                measureTimeMillis {
                    result = service.doRemoteFunction(value)
                }.also { log.info("Time taken (ms): $it") }

                if (result == RemoteServiceStatus.FAILURE.remoteServiceStatusValue) {
                    log.info("Failed to reach remote: (${i + 1})")
                    retries++
                    try {
                        Thread.sleep(DELAY_MS)
                    } catch (e: InterruptedException) {
                        log.error("Thread sleep state interrupted", e)
                        Thread.currentThread().interrupt()
                    }
                } else break

                i++; }

            return result
        }
    }

    open class Client {
        private val serviceAmbassador: IRemoteService = ServiceAmbassador()

        /**
         * The ambassador pattern creates a helper service that sends network requests
         * on behalf of a client. It is often used in cloud-based applications to offload features of a remote service.
         *
         * <p>An ambassador service can be thought of as an out-of-process proxy that is co-located with the client.
         * Similar to the proxy design pattern, the ambassador service provides an interface for another remote service.
         * In addition to the interface, the ambassador provides extra functionality and features,
         * specifically offloaded common connectivity tasks. This usually consists of monitoring, logging,
         * routing, security etc. This is extremely useful in legacy application where the codebase is
         * difficult to modify and allows for improvements in the application's networking capabilities.
         */
        open fun useService(value: Int): Long = serviceAmbassador
            .doRemoteFunction(value)
            .also { log.info("Service result: $it") }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val host1: Client = Client()
        val host2: Client = Client()
        host1.useService(12)
        host2.useService(73)
    }
}

typealias IRandomProvider = () -> Double