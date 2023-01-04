package cfcodefans.study.spring_field.spring.examples

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong


data class Greeting(val id: Long, val content: String)

@RestController
open class GreetingCtl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GreetingCtl::class.java)
    }

    val counter: AtomicLong = AtomicLong()

    @GetMapping("/greeting")
    open fun greeting(@RequestParam(value = "name", defaultValue = "World") name: String): Greeting {
        return Greeting(id = counter.incrementAndGet(), "Hello, $name!")
    }
}

@SpringBootApplication
open class GSRestServiceApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GSRestServiceApp::class.java)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(GSRestServiceApp::class.java, *args)
}


/**
 * refers to https://spring.io/guides/gs/rest-service/
 * https://github.com/spring-guides/gs-rest-service/blob/main/complete/src/test/java/com/example/restservice/GreetingControllerTests.java
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [GSRestServiceApp::class])
@AutoConfigureMockMvc
open class RestServiceTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RestServiceTests::class.java)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun noParamGreetingShouldReturnDefaultMessage() {
        mockMvc.perform(get("/greeting"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Hello, World!"))
    }

    @Test
    fun paramGreetingShouldReturnTailoredMessage() {
        mockMvc.perform(get("/greeting").param("name", "Spring Community"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Hello, World!"))
    }
}