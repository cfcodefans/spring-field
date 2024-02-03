package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Balking {
    private val log: Logger = LoggerFactory.getLogger(Balking::class.java)

    enum class WashingMachineState {
        ENABLED, WASHING
    }

    fun interface IDelayProvider {
        fun executeAfterDelay(interval: Long,
                              timeUnit: TimeUnit,
                              task: Runnable)
    }

    open class WashingMachine(private val delayProvider: IDelayProvider = IDelayProvider { interval, timeUnit, task ->
        try {
            Thread.sleep(timeUnit.toMillis(interval))
        } catch (ie: InterruptedException) {
            log.error("", ie)
            Thread.currentThread().interrupt()
        }
        task.run()
    }) {
        var state: WashingMachineState = WashingMachineState.ENABLED
            private set

        /**
         * Method responsible for washing if the object is in appropriate state.
         */
        fun wash(): Unit {
            synchronized(this) {
                log.info("Actual machine state: ${this.state}")
                if (state == WashingMachineState.WASHING) {
                    log.error("Cannot wash if the machine has been already washing!")
                    return
                }
                this.state = WashingMachineState.WASHING
            }
            log.info("Doing the washing")
            delayProvider.executeAfterDelay(50, TimeUnit.MILLISECONDS, this::endOfWashing)
        }

        @Synchronized
        fun endOfWashing(): Unit {
            state = WashingMachineState.ENABLED
            log.info("Washing completed")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val washingMachine: WashingMachine = WashingMachine()
        val executorService: ExecutorService = Executors.newFixedThreadPool(3)

        repeat(3) {
            executorService.execute(washingMachine::wash)
        }
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
                executorService.shutdownNow()
        } catch (ie: InterruptedException) {
            log.error("ERROR:Waiting on executor service shutdown@")
            Thread.currentThread().interrupt()
        }

    }
}