package cfcodefans.study.spring_field.spring.examples

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import java.net.URI

@JsonIgnoreProperties(ignoreUnknown = false)
data class Value(var id: Long? = null, var quote: String? = null)

@JsonIgnoreProperties(ignoreUnknown = false)
data class Quote(var type: String? = null, var quote: Value? = null)

@SpringBootApplication
open class ConsumingRestApp {
    @Bean
    open fun restTemplate(builder: RestTemplateBuilder): RestTemplate = builder.build()
}

/**
 * refers to https://github.com/spring-guides/gs-consuming-rest/tree/main/complete
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ConsumingRestApp::class])
open class ConsumingRestTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ConsumingRestTests::class.java)
    }

    @Autowired
    open lateinit var restTemplate: RestTemplate

    @Test
    fun testApiCall() {
        val quoteRe: Quote? = restTemplate.getForObject("http://localhost:8080/api/random", Quote::class.java)
        log.info(quoteRe.toString())
    }

    @Test
    fun testHeader() {
        val respEntity: ResponseEntity<Quote> = restTemplate.exchange(RequestEntity<Quote>(HttpMethod.GET, URI("http://localhost:8080/api/random")), Quote::class.java)
        log.info(respEntity.toString())
    }
}