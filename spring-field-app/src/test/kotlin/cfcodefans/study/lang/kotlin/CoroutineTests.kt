package cfcodefans.study.lang.kotlin

import cfcodefans.study.spring_field.commons.Jsons
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test


/**
 * refers to https://kotlinlang.org/docs/coroutines-basics.html
 */
open class CoroutineTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CoroutineTests::class.java)
        fun Job.info(): JsonNode = Jsons.toNode(mapOf(
                "clz" to this.javaClass.name,
                "nativeStr" to this.toString(),
                "isActive" to this.isActive,
                "isCompleted" to this.isCompleted,
                "isCancelled" to this.isCancelled,
                "parent" to this.parent?.info()
        ))
    }

    @Test
    fun `your first coroutine`() {
        runBlocking {
            val job: Job = launch {
                delay(1000)
                log.info("World!")
                log.info(this.toString())
            }
            log.info("Hello")
            log.info(job.info().toPrettyString())
        }
        log.info("Done")
    }
}