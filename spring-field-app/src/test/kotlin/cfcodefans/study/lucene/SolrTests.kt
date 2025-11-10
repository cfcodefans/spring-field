package cfcodefans.study.lucene

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class SolrTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SolrTests::class.java)

        @BeforeAll
        @JvmStatic
        fun init() {
            log.info("init")
        }

        @AfterAll
        @JvmStatic
        fun cleanUp() {
            log.info("cleanUp")
        }
    }
}