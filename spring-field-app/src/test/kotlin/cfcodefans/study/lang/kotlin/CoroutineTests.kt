package cfcodefans.study.lang.kotlin

import cfcodefans.study.spring_field.commons.Jsons
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test


/**
 * refers to https://kotlinlang.org/docs/coroutines-basics.html
 */
open class CoroutineTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CoroutineTests::class.java)
        fun Job.info(): JsonNode = mapOf("clz" to this.javaClass.name,
                                         "nativeStr" to this.toString(),
                                         "isActive" to this.isActive,
                                         "isCompleted" to this.isCompleted,
                                         "isCancelled" to this.isCancelled,
                                         "parent" to this.parent?.info())
            .let { Jsons.toNode(it) }

        fun CoroutineContext.info(): JsonNode = mapOf("clz" to this.javaClass.name,
                                                      "nativeStr" to toString(),
                                                      "isActive" to this.isActive,
                                                      "job" to this.job.info())
            .let { Jsons.toNode(it) }

        fun CoroutineScope.info(): JsonNode = mapOf("clz" to this.javaClass.name,
                                                    "nativeStr" to this.toString(),
                                                    "isActive" to this.isActive,
                                                    "context" to this.coroutineContext.info())
            .let { Jsons.toNode(it) }
    }

    @Test
    fun `your first coroutine`() {
        runBlocking {
            launch {
                delay(1000)
                log.info("World!")
                log.info(this.info().toPrettyString())
            }
            log.info("Hello")
            log.info(this.info().toPrettyString())
        }
        log.info("Done")
    }
}