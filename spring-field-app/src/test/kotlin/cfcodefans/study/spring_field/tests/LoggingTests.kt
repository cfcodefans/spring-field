package cfcodefans.study.spring_field.tests

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class LoggingTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LoggingTests::class.java)
    }

    @Test
    fun testLog4j2() {
        log.trace("test trace")
        log.debug("test debug")
        log.info("test info")
        log.warn("test warn")
        log.error("test error")
    }
}