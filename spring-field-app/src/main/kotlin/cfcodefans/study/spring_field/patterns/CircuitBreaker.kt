package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

        open class DefaultCircuitBreaker(private val service: IRemoteService,
                                         private val timeout: Long,
                                         private val failureThreshold: Int,
                                         private val retryTimePeriod: Long) : ICircuitBreaker {

            override var state: State = State.CLOSED
                get() = evaluateState().state
                set(value) {
                    field = value
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
            var lastFailureResp: String? = null

            override fun recordSuccess() = apply {
                failureCount = 0
                lastFailureTime = System.nanoTime() + futureTime
                state = State.CLOSED
            }

            override fun recordFailure(resp: String): ICircuitBreaker = apply {
                failureCount++
                lastFailureTime = System.nanoTime()
                //Cache the failure response for returning on open state
                lastFailureResp = resp
            }

            //Evaluate the current state based on failureThreshold,
            //failureCount and lastFailureTime.
            protected fun evaluateState(): ICircuitBreaker = apply {
                this.state = if (failureCount >= failureThreshold) {//Then something is wrong with remote service
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
                    kotlin.runCatching { service.call() }
                        .onSuccess { recordSuccess() }
                        .onFailure {
                            recordFailure(it.message!!)
                            throw it
                        }.getOrNull()
                }
            }
        }
    }
}