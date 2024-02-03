package cfcodefans.study.spring_field.tests

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

open class CoroutineTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CoroutineTests::class.java)

        data class Account(val id: Long,
                           var name: String,
                           var balance: BigDecimal = BigDecimal.ZERO,
                           var createdAt: LocalDateTime,
                           var updatedAt: LocalDateTime)

        enum class TxStatus {
            PENDING, SUCCEEDED, FAILED
        }

        enum class TxKind {
            Tip, Gift, Fee, Reward, Payout
        }

        data class Transaction(val id: Long,
                               val senderUid: Long,
                               val receiverUid: Long,
                               val kind: TxKind,
                               val amount: BigDecimal,
                               val note: String,
                               var status: TxStatus,
                               var createdAt: LocalDateTime,
                               var updatedAt: LocalDateTime)

    }

    @Test
    fun testRunBlocking(): Unit {
        runBlocking {
            delay(1000)
            log.info("World")
        }
        runBlocking {
            delay(500)
            log.info("Hey")
        }
        log.info("Hello")
    }

    @Test
    fun testPipeline(): Unit {
        val ch: Channel<LocalDateTime> = Channel(capacity = 10)
        log.info("start pipeline tests")

        runBlocking {
//            log.info("ch.size: ${ch.toList().size}")
            log.info("start sending")
            launch {
                repeat(3) { i ->
                    delay(1000)
                    ch.send(LocalDateTime.now().also { log.info("\tsend $i times with ${it}") })
                }
            }

            log.info("start receiving")
            launch {
                repeat(6) { i ->
                    delay(500)
                    val received: LocalDateTime? = ch.tryReceive().getOrElse { null }
                    log.info("\t\treceived $i times with ${received}")
                }
            }
        }
        ch.close()
    }

}
