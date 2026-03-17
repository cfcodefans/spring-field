package cfcodefans.study.lang.kotlin

import cfcodefans.study.spring_field.commons.Jsons
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis
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

    /**
     * refers to https://youtu.be/0Hv5LTxAutw?si=E9su0zohkAi6oYkG
     */
    @Test
    fun `heavy cpu computation`() {
        log.info("heavy cpu computation start {")
        measureTimeMillis {
            (1..100_000_000).forEach { sqrt(it.toDouble()) }
        }.let { log.info("} heavy cpu computation took $it ms") }
    }

    @Test
    fun `heavy cpu computation in thread`() {
        log.info("heavy cpu computation start in thread {")
        measureTimeMillisWithResult {
            (1..3).map {
                thread {
                    this.`heavy cpu computation`()
                }
            }
        }.also { log.info("} heavy cpu computation in thread took ${it.first} ms") }
            .also { it.second.forEach { t -> t.join() } }
    }

    suspend fun `heavy cpu computation in suspend function`() {
        log.info("heavy cpu computation start in suspend function {")
        measureTimeMillis {
            this.`heavy cpu computation`()
        }.let { log.info("} heavy cpu computation took in suspend function $it ms") }
    }

    @Test
    fun `heavy cpu computation in Coroutine`() {
        log.info("heavy cpu computation start in Coroutine {")
        measureTimeMillisWithResult {
            CoroutineScope(Dispatchers.Default).launch {
                (1..3).map {
                    launch {
                        `heavy cpu computation in suspend function`()
                    }
                }
            }
        }.also { job ->
            runBlocking {
                job.second.join()
            }
        }.also { log.info("} heavy cpu computation in Coroutine took ${it.first} ms") }
    }

    @Test
    fun `heavy cpu computation in Coroutine batch`() {
        log.info("heavy cpu computation start in Coroutine batch {")
        measureTimeMillisWithResult {
            CoroutineScope(Dispatchers.Default).launch {
                (1..3).map {
                    `heavy cpu computation in suspend function`()
                }
            }
        }.also { log.info("} heavy cpu computation in Coroutine batch took ${it.first} ms") }
            .let { job ->
                runBlocking {
                    job.second.join()
                }
            }
    }
}