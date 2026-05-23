package cfcodefans.study.spring_field.api.restful

//import com.turkraft.springfilter.boot.PageSortAutoConfiguration
import cfcodefans.study.spring_field.commons.Jsons3
import cfcodefans.study.spring_field.commons.TrafficLogFilter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.PostConstruct
import jakarta.servlet.annotation.WebFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.graphql.autoconfigure.security.GraphQlWebMvcSecurityAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Schema(name = "Data")
data class DataDTO(val id: Long = ID_GEN.incrementAndGet(),
                   var name: String,

                   var data: JsonNode? = null,
                   var note: JsonNode? = null,
                   var tags: List<String> = emptyList(),

                   val createdAt: LocalDateTime = LocalDateTime.now(),
                   var updatedAt: LocalDateTime = LocalDateTime.now()) {

    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        val ID_GEN: AtomicLong = AtomicLong(0)
    }

    override fun toString(): String = Jsons3.toString(this)
}

@Component
@WebFilter
open class TestLogFilter : TrafficLogFilter()

//(scanBasePackages = [RestfulApiApp.BASE_PACKAGE],
//                       exclude = [PageSortAutoConfiguration::class])
//@ComponentScan(RestfulApiApp.BASE_PACKAGE)
//@AutoCfgWithoutDataJpa
//@AutoCfgWithoutSecurity
@SpringBootApplication(scanBasePackages = [RestfulApiApp.BASE_PACKAGE], exclude = [
//    PageSortAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,

    SecurityAutoConfiguration::class,
    UserDetailsServiceAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
    GraphQlWebMvcSecurityAutoConfiguration::class,
])
open class RestfulApiApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RestfulApiApp::class.java)
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.api.restful"
        const val PORT: Int = 8082

    }

    @PostConstruct
    open fun logStartupHints() {
        log.info("""starting
            http://localhost:${PORT}/swagger-ui/index.html
        """.trimIndent())
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(RestfulApiApp::class.java,
                          *args,
                          "--spring.profiles.active=restful-api",
                          "--server.port=${RestfulApiApp.PORT}",
                          "--server.compression.enabled=true")
}

@RestController
@RequestMapping("/default")
open class RestfulApiCtrl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RestfulApiCtrl::class.java)
    }

    @GetMapping("/data")
    open fun getData(): DataDTO {
        return DataDTO(name = "foo",
                       data = Jsons3.toNode(mapOf("x" to 100,
                                                  "y" to 200,
                                                  "z" to 200,
                                                  "enable" to true)),
                       note = Jsons3.MAPPER.createObjectNode())
    }
}