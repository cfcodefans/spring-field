package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Callback {
    private val log: Logger = LoggerFactory.getLogger(Callback::class.java)

    fun interface ICallback {
        fun call(): Unit
    }

    abstract class Task {
        fun executeWith(callback: ICallback?): Unit {
            execute()
            callback?.call()
        }

        abstract fun execute(): Unit
    }

    class SimpleTask : Task() {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(SimpleTask::class.java)
        }

        override fun execute() {
            log.info("Perform some important activity and after call the callback method.")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val task: SimpleTask = SimpleTask()
        task.executeWith() {
            log.info("I'm done now.")
        }

    }

}