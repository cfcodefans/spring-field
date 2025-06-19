package cfcodefans.study.spring_field.standalone_web.template

import cfcodefans.study.spring_field.standalone_web.template.StandaloneWebApp.Companion.BASE_PACKAGE
import cfcodefans.study.spring_field.standalone_web.template.StandaloneWebApp.Companion.PORT
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.nio.file.*
import kotlin.io.path.isReadable
import kotlin.io.path.notExists
import kotlin.io.path.readBytes


@SpringBootApplication(scanBasePackages = [BASE_PACKAGE])
@EnableCaching
open class StandaloneWebApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(StandaloneWebApp::class.java)
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.standalone_web.template"
        const val PORT: Int = 8081
    }
}

@Configuration
open class OpenAPIDocConfig {
    companion object {
        val log: Logger = LoggerFactory.getLogger(OpenAPIDocConfig::class.java)
    }

    @PostConstruct
    open fun init() {
        log.info("""OpenAPIDocConfig is initializing...
            |check with http://localhost:8081/swagger-ui/index.html#/biz-ctrl/getCurrentDateTime
        """.trimMargin())
    }

    @Bean
    open fun createRestApi(): GroupedOpenApi = GroupedOpenApi
        .builder()
        .group("public-api")
        .pathsToExclude("/auth")
        .build()
}

fun main(args: Array<String>) {
    SpringApplication.run(StandaloneWebApp::class.java,
                          *args,
                          "--server.port=$PORT",
                          "--server.compression.enabled=true",
                          "--server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml,application/x-javascript",
                          "--server.compression.min-response-size=2048",
                          "--springdoc.api-docs.enabled=true",
                          "--springdoc.swagger-ui.enabled=true",
                          "--springdoc.api-docs.path=/api-docs")
}

@ControllerAdvice
open class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    companion object {
        private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

        private fun logReq(ex: Exception, status: HttpStatus, req: WebRequest) {
            log.error("""Exception ${ex.message} thrown at
                Request to 
                Url: ${req.contextPath}
                with parameters: ${req.parameterMap}
                from client: ${req.getDescription(true)}
                responded with $status""".trimIndent(), ex)
        }

        private fun exToStatus(ex: Throwable): HttpStatus = when (ex) {
            is IllegalArgumentException -> HttpStatus.BAD_REQUEST
            is NoSuchElementException -> HttpStatus.NOT_FOUND
            is UnsupportedOperationException -> HttpStatus.NOT_IMPLEMENTED
            is IllegalStateException -> HttpStatus.EXPECTATION_FAILED
            is IllegalAccessException -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    @ExceptionHandler(value = [IllegalArgumentException::class,
        NoSuchElementException::class,
        RuntimeException::class,
        UnsupportedOperationException::class,
        Exception::class])
    fun handleException(ex: Exception,
                        req: WebRequest,
                        resp: HttpServletResponse): ResponseEntity<Any> {

        logReq(ex = ex, status = HttpStatus.valueOf(resp.status), req = req)

        return ResponseEntity(ex.message ?: ex.localizedMessage, exToStatus(ex))
    }
}

@RestController
open class StaticHost {
    companion object {
        val log: Logger = LoggerFactory.getLogger(StaticHost::class.java)
        const val STATIC_ROOT: String = "src/main/kotlin/cfcodefans/study/spring_field/standalone_web/template/static"
        const val CACHE_NAME: String = "static-cache"
    }

    @Autowired
    open lateinit var cacheManager: CacheManager

    open val rootPath: String = STATIC_ROOT

    @PostConstruct
    open fun init() {
        Thread() {
            FileSystems.getDefault()
                .newWatchService()
                .use { ws ->
                    Paths.get(rootPath)
                        .register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE)
                    while (true) {
                        val watchKey: WatchKey = ws.take()
                        for (ev in watchKey.pollEvents()) {
                            val pathStr: String = ev.context().toString()
                            cacheManager.getCache(CACHE_NAME)?.evictIfPresent(pathStr)
                            log.info("$pathStr evicted")
                        }
                    }
                }
        }.also { it.isDaemon = true }
            .start()
    }

    @GetMapping("/static/{path}")
    @Cacheable(value = [CACHE_NAME], key = "#path")
    open fun serve(@PathVariable(name = "path", required = true) path: String): ResponseEntity<ByteArray> = Paths
        .get(STATIC_ROOT)
        .resolve(path)
        .let { p ->
            if (p.notExists()) throw NoSuchElementException("path: $p is not found")
            if (p.isReadable().not()) throw IllegalAccessException("path: $p is not accessible")

            ResponseEntity<ByteArray>(p.readBytes(),
                                      HttpHeaders().apply { add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(p)) },
                                      HttpStatus.OK)
        }
}

@Configuration
@EnableWebSecurity
open class SecurityConfig {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

    @Bean(name = ["protectedResMatcher"])
    @Qualifier("protectedResMatcher")
    open fun apiRequestMatcher(): RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/api/**")

    @Bean
    open fun authRequestMatcher(): RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/auth/login")

    @Bean
    open fun staticResourceMatcher(): RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/static/**")

    @Throws(java.lang.Exception::class)
    @Bean
    open fun configure(http: HttpSecurity): SecurityFilterChain = http
        .csrf { customizer -> customizer.disable() }
        .addFilterBefore(JwtFilter(reqMatcher = apiRequestMatcher()), UsernamePasswordAuthenticationFilter::class.java)
        .authorizeHttpRequests { customizer ->
            customizer.requestMatchers(authRequestMatcher(), staticResourceMatcher())
                .permitAll()
                .requestMatchers(apiRequestMatcher())
                .authenticated()
                .anyRequest()
                .permitAll()
        }.build()
        .also { log.info(it.toString()) }
}

