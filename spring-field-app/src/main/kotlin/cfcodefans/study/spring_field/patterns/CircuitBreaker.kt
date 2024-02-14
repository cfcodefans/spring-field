package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object CircuitBreaker {
    private val log: Logger = LoggerFactory.getLogger(CircuitBreaker::class.java)

    enum class State {
        CLOSED, OPEN, HALF_OPEN
    }

    open class RemoteServiceException(msg: String) : Exception(msg)

    fun interface IRemoteService {
        @Throws(RemoteServiceException::class)
        fun call(): String
    }

    open class QuickRemoteService : IRemoteService {
        override fun call(): String = "Quick Service is working"
    }

    open class DelayedRemoteService(private val serverStartTime: Long = System.nanoTime(),
                                    private val delay: Int = 20) : IRemoteService {

        /**
         * Responds based on delay, current time and server start time if the service is down/working
         * @return The state of the service
         */
        @Throws(RemoteServiceException::class)
        override fun call(): String {
            val now: Long = System.nanoTime()
            //Since currentTime and serverStartTime are both in nanoseconds, we convert it to
            //seconds by diving by 10e9 and ensure floating point division by multiplying it
            //with 1.0 first. We then check if it is greater or less than specified delay and then
            //send the reply
            if ((now - serverStartTime) * 1.0 / (1000 * 1000 * 1000) < delay) {
                //Can use Thread.sleep() here to block and simulate a hung server
                throw RemoteServiceException("Delayed service is down")
            }
            return "Delayed service is working"
        }

        interface ICircuitBreaker {
            // Success response. Reset everything to defaults
            fun recordSuccess(): ICircuitBreaker

            // Failure response. Handle accordingly with response and change state if required.
            fun recordFailure(resp: String): ICircuitBreaker

            // Set/Get the current state of circuit breaker
            var state: State

            // Attempt to fetch response from the remote service.
            @Throws(RemoteServiceException::class)
            fun attemptReq(): String?
        }

        /**
         * The delay based Circuit breaker implementation that works
         * in a CLOSED -> OPEN - (retry_time_period) -> HALF_OPEN -> CLOSED flow with some retry time period for failed
         * services and a failure threshold for service to open circuit
         *
         * Constructor to create an instance of Circuit Breaker.
         * @param timeout           Timeout for the API request. Not necessary for this simple example
         * @param failureThreshold  Number of failures we receive from the depended on service before
         *                          changing state to "OPEN"
         * @param retryTimePeriod   Time, in nanoseconds, period after which a new request is made to remote service
         *                          for status check
         */
        open class DefaultCircuitBreaker(private val service: IRemoteService,
                                         private val timeout: Long,
                                         private val failureThreshold: Int,
                                         private val retryTimePeriod: Long) : ICircuitBreaker {

            private var _state: State = State.CLOSED
            override var state: State = _state
                get() = evaluateState().let { _state }
                /**
                 * Break the circuit beforehand if it is known service is down or connect the circuit manually
                 * if service comes online before expected.
                 * @param state State at which circuit is in
                 */
                set(value) {
                    _state = value
                    when (field) {
                        State.OPEN -> {
                            this.failureCount = failureThreshold
                            this.lastFailureTime = System.nanoTime()
                        }
                        State.HALF_OPEN -> {
                            this.failureCount = failureThreshold
                            this.lastFailureTime = System.nanoTime() - retryTimePeriod
                        }
                        else -> this.failureCount = 0
                    }
                }

            private val futureTime: Long = 1000_000_000_000
            var lastFailureTime: Long = System.nanoTime() + futureTime
            var failureCount: Int = 0
                set(value) {
                    log.info("${this.hashCode()} failureCount: $field to be set to $value")
                    field = value
                }
            var lastFailureResp: String? = null

            /**
             * Reset everything to defaults
             */
            override fun recordSuccess() = apply {
                log.info("$this failureCount: $failureCount")
                failureCount = 0
                lastFailureTime = System.nanoTime() + futureTime
                state = State.CLOSED
            }

            override fun recordFailure(resp: String): ICircuitBreaker = apply {
                log.info("$this failureCount: $failureCount")
                this.failureCount += 1
                lastFailureTime = System.nanoTime()
                //Cache the failure response for returning on open state
                lastFailureResp = resp
            }

            //Evaluate the current state based on failureThreshold,
            //failureCount and lastFailureTime.
            protected fun evaluateState(): ICircuitBreaker = apply {
                _state = if (failureCount >= failureThreshold) {//Then something is wrong with remote service
                    if ((System.nanoTime() - lastFailureTime) > retryTimePeriod) {
                        // We have waited long enough and should try checking if service is up
                        State.HALF_OPEN
                    } else { //Service would still probably be down
                        State.OPEN
                    }
                } else State.CLOSED //Everything is working fine
            }

            @Throws(RemoteServiceException::class)
            override fun attemptReq(): String? {
                evaluateState()
                return if (state == State.OPEN) this.lastFailureResp //return cached response if the circuit is in OPEN state
                else {
                    //Make the API request if the circuit is not OPEN
                    runCatching {
                        // In a real application, this would be run in a thread and the timeout
                        // parameter of the circuit breaker would be utilized to know if service is working.
                        // Here, we simulate that based on server response itself
                        service.call()
                    }.onSuccess { recordSuccess() }
                        .onFailure {
                            recordFailure(it.message!!)
                            throw it
                        }.getOrNull()
                }
            }
        }


        open class MonitoringService(val delayedService: ICircuitBreaker,
                                     val quickService: ICircuitBreaker) {
            //Assumption: Local service won't fail, no need to wrap it in a circuit break logic
            open fun localResourceResp(): String = "Local Service is working"

            /**
             * Fetch response from the delayed service (with some simulated startup time).
             * @return response string
             */
            open fun delayedServiceResp(): String? =
                runCatching { delayedService.attemptReq() }
                    .onFailure { e -> return e.message }
                    .getOrNull()

            /**
             * Fetches response from a healthy service without any failure
             * @return response string
             */
            open fun quickServiceResp(): String? =
                runCatching { quickService.attemptReq() }
                    .onFailure { e -> return e.message }
                    .getOrNull()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        var serverStartTime: Long = System.nanoTime()
        var delayedService: IRemoteService = DelayedRemoteService(serverStartTime, 5)
        var delayedServiceCircuitBreaker: DelayedRemoteService.ICircuitBreaker = DelayedRemoteService.DefaultCircuitBreaker(service = delayedService,
                timeout = 3000,
                failureThreshold = 2,
                retryTimePeriod = 2000_000_000)

        var quickService: IRemoteService = QuickRemoteService()
        var quickServiceCircuitBreaker: DelayedRemoteService.ICircuitBreaker = DelayedRemoteService.DefaultCircuitBreaker(service = quickService,
                timeout = 3000,
                failureThreshold = 2,
                retryTimePeriod = 2000_000_000)

        // Create an object of monitoring service which makes both local and remote calls
        var monitoringService: DelayedRemoteService.MonitoringService = DelayedRemoteService.MonitoringService(
                delayedService = delayedServiceCircuitBreaker,
                quickService = quickServiceCircuitBreaker)

        log.info("""Fetch response from local resource
            ${monitoringService.localResourceResp()}
        """.trimIndent())

        log.info("""Fetch response from delayed service 2 times, to meet the failure threshold
            1. ${monitoringService.delayedServiceResp()}
            2. ${monitoringService.delayedServiceResp()}
        """.trimIndent())

        log.info("""Fetch current state of delayed service circuit breaker after crossing failure threshold limit
            which is OPEN now: 
            ${delayedServiceCircuitBreaker.state}
        """.trimIndent())

        log.info("""Meanwhile, the delayed service is down, fetch response from the healthy quick service
            ${monitoringService.quickServiceResp()}
            ${quickServiceCircuitBreaker.state}
        """.trimIndent())

        runCatching {
            repeat(5) {
                log.info("Waiting for delayed service to become responsive in $it seconds")
                Thread.sleep(1000)
            }
        }

        log.info("""Check the state of delayed circuit breaker, should be HALF_OPEN
               ${delayedServiceCircuitBreaker.state}
        """.trimIndent())

        log.info("""Fetch response from delayed service, which should be healthy by now
            ${monitoringService.delayedServiceResp()}
        """.trimIndent())

        log.info("""As successful response is fetched, it should be CLOSED again.
            ${delayedServiceCircuitBreaker.state}
        """.trimIndent())
    }
}