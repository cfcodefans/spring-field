package cfcodefans.study.spring_field.graphql.standard

import cfcodefans.study.spring_field.commons.TrafficLogFilter
import jakarta.annotation.PostConstruct
import jakarta.servlet.annotation.WebFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@SpringBootApplication(scanBasePackages = [GraphQLWebApp.BASE_PACKAGE])
@EnableJpaAuditing
open class GraphQLWebApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphQLWebApp::class.java)
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.graphql.standard"
        const val PORT: Int = 8082
    }

    @PostConstruct
    open fun logStartupHints() {
        log.info(
            "GraphQLWebApp — try GraphiQL at http://localhost:${PORT}/graphiql and POST /graphql (profile graphql-web).",
        )
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(
        GraphQLWebApp::class.java,
        *args,
        "--spring.profiles.active=graphql-web",
        "--server.port=${GraphQLWebApp.PORT}",
        "--server.compression.enabled=true",
    )
}

@Configuration
@EnableWebSecurity
open class GraphQLWebSecurity {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphQLWebSecurity::class.java)
    }

    @Bean
    open fun graphqlSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { csrfConfigurer: CsrfConfigurer<HttpSecurity> -> csrfConfigurer.disable() }
            .authorizeHttpRequests { registry: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry ->
                registry.anyRequest().permitAll()
            }
            .build()
            .also { chain: SecurityFilterChain ->
                log.info("GraphQLWebApp security: all requests permitted (study app). $chain")
            }
}

@ControllerAdvice
open class GraphQLWebApiExceptionHandler {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLWebApiExceptionHandler::class.java)
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        NoSuchElementException::class,
        Exception::class,
    )
    fun handle(ex: Exception, req: WebRequest): ResponseEntity<String> {
        log.error("Request {} failed: {}", req.getDescription(false), ex.message, ex)
        val status: HttpStatus = when (ex) {
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            is NoSuchElementException -> HttpStatus.NOT_FOUND
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        val body: String = ex.message ?: ex.toString()
        return ResponseEntity(body, status)
    }
}

@Component
@WebFilter
open class TestLogFilter : TrafficLogFilter() {}