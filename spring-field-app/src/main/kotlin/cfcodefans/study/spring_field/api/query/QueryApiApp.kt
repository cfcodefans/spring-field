package cfcodefans.study.spring_field.api.query

import cfcodefans.study.spring_field.RepoTestSpringProps
import cfcodefans.study.spring_field.commons.TrafficLogFilter
//import com.turkraft.springfilter.boot.PageSortAutoConfiguration
import jakarta.annotation.PostConstruct
import jakarta.servlet.annotation.WebFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.graphql.autoconfigure.security.GraphQlWebMvcSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest

@SpringBootApplication(scanBasePackages = [QueryApiApp.BASE_PACKAGE],
                       exclude = [
                           SecurityAutoConfiguration::class,
                           UserDetailsServiceAutoConfiguration::class,
                           ServletWebSecurityAutoConfiguration::class,
                           GraphQlWebMvcSecurityAutoConfiguration::class,
//                           PageSortAutoConfiguration::class,
                       ])
@EnableJpaAuditing
@SpringBootConfiguration
open class QueryApiApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(QueryApiApp::class.java)
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.api.query"
        const val PORT: Int = 8082
    }

    @PostConstruct
    open fun logStartupHints() {
        log.info("""GraphQLWebApp — 
            |GraphiQL http://localhost:${PORT}/graphiql/ ; 
            |POST http://localhost:${PORT}/graphql ; 
            |OData http://localhost:${PORT}/odata/v4/GraphEntities?\filter=entityType eq 'file' (profile graphql-web).
            |spring-filter http://localhost:${PORT}/swagger-ui/index.html""".trimMargin())
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(QueryApiApp::class.java,
                          *args,
                          "--spring.profiles.active=graphql-web",
                          "--server.port=${QueryApiApp.PORT}",
                          "--server.compression.enabled=true",
                          RepoTestSpringProps.ACTIVE_PROFILE_LAB,
                          RepoTestSpringProps.DATASOURCE_URL,
                          RepoTestSpringProps.DATASOURCE_DRIVER,
                          RepoTestSpringProps.DATASOURCE_USERNAME,
                          RepoTestSpringProps.DATASOURCE_PASSWORD,
                          RepoTestSpringProps.JPA_DDL_AUTO,
                          RepoTestSpringProps.JPA_SHOW_SQL,
                          RepoTestSpringProps.JPA_SHOW_SQL_FORMAT,
                          RepoTestSpringProps.JPA_OPEN_IN_VIEW,
                          RepoTestSpringProps.JPA_TIME_ZONE,
//                          RepoTestSpringProps.JACKSON_2_DEFAULT,
//                          "spring.http.converters.preferred-json-mapper=jackson2"
    )
}

@ControllerAdvice
open class GraphQLWebApiExceptionHandler {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(GraphQLWebApiExceptionHandler::class.java)
    }

    @ExceptionHandler(IllegalArgumentException::class,
                      NoSuchElementException::class,
                      Exception::class)
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
open class TestLogFilter : TrafficLogFilter()


@RestController
@RequestMapping("/default")
open class DefaultGraphqlCtrl(private val repo: IGraphEntityRepo) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DefaultGraphqlCtrl::class.java)
    }

    @GetMapping("/top-{pageSize}")
    open fun getTopNGraphEntity(@PathVariable("pageSize") pageSize: Int): ResponseEntity<List<GraphEntity>> {
        return repo
            .findTopN(pageSize)
            .let { ResponseEntity.ok(it) }
    }
}