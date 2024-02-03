package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.jvm.Throws

object ActiveObject {
    private val log: Logger = LoggerFactory.getLogger("ActiveObject")

    abstract class ActiveCreature(val name: String) {
        var status: Int = 0
            set(value) {}

        private val reqs: BlockingQueue<Runnable> = LinkedBlockingQueue()

        private val thread: Thread = Thread() {
            while (Thread.currentThread().isInterrupted.not()) try {
                reqs.take().run()
            } catch (e: InterruptedException) {
                if (status != 0) {
                    log.error("Thread was interrupted. --> ${e.message}")
                }
                Thread.currentThread().interrupt()
            }
        }.also { it.start() }

        @Throws(InterruptedException::class)
        fun eat() = reqs.put() {
            log.info("${name} is eating")
            log.info("${name} has finished eating!")
        }


        @Throws(InterruptedException::class)
        fun roam() = reqs.put() { log.info("${name} has started to roam in the wastelands.") }

        fun kill(status: Int) {
            this.status = status
            thread.interrupt()
        }
    }

    class Orc(name: String) : ActiveCreature(name)

    @JvmStatic
    fun main(args: Array<String>) {
        val creatures: MutableList<ActiveCreature> = arrayListOf()
        try {
            repeat(3) { i ->
                Orc(Orc::class.simpleName + i)
                    .also { creatures.add(it) }
                    .also {
                        it.eat()
                        it.roam()
                    }
            }
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            log.error(e.message)
            Thread.currentThread().interrupt()
        } finally {
            for (c in creatures) c.kill(0)
        }
    }
}