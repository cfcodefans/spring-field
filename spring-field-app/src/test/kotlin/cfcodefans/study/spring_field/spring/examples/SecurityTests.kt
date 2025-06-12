package cfcodefans.study.spring_field.spring.examples.security

import cfcodefans.study.spring_field.spring.examples.security.SecurityTestsApp.Companion.BASE_PACKAGE
import cfcodefans.study.spring_field.spring.examples.security.SecurityTestsApp.Companion.PORT
import cfcodefans.study.spring_field.standalone_web.template.JWTHelper
import cfcodefans.study.spring_field.standalone_web.template.JwtFilter.Companion.TOKEN_COOKIE_KEY
import io.jsonwebtoken.Claims
import io.jsonwebtoken.io.IOException
import jakarta.annotation.PostConstruct
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.WebUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api")
open class BizCtrl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BizCtrl::class.java)
        val DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @GetMapping("/now")
    fun getCurrentDateTime(): String = LocalDateTime.now().format(DATETIME_FMT)
}

data class AuthReq(val username: String, val pwd: String)
data class AuthResp(val token: String)

@RestController
open class AuthRes {
    @Autowired
    open lateinit var authCfg: AuthenticationConfiguration

    @Autowired
    open lateinit var mockUserDetailService: UserDetailsService

    @Throws(Exception::class)
    @PostMapping(path = ["/authenticate"],
                 consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
                 produces = [MediaType.TEXT_PLAIN_VALUE])
    open fun login(@RequestBody req: AuthReq, resp: HttpServletResponse): ResponseEntity<AuthResp> {
        try {
            authCfg.authenticationManager.authenticate(UsernamePasswordAuthenticationToken(req.username, req.pwd))
        } catch (e: BadCredentialsException) {
            throw Exception("invalid username or password", e)
        }
        return mockUserDetailService.loadUserByUsername(req.username)
            .let { userDetails -> JWTHelper.generateToken(userDetails.username) }
            .also { token -> resp.setHeader(HttpHeaders.SET_COOKIE, "$TOKEN_COOKIE_KEY=$token; Path=/; HttpOnly; Secure; SameSite=Strict") }
            .let { token -> ResponseEntity.ok(AuthResp(token!!)) }
    }
}

@Service
open class MockUserDetailService : UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails = User("user", "password", emptyList())
}

//@Component
open class JwtFilter(open val mockUserDetailService: UserDetailsService,
                     open val reqMatcher: RequestMatcher) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(req: HttpServletRequest,
                                  resp: HttpServletResponse,
                                  filterChain: FilterChain) {
        if (reqMatcher.matches(req).not()) {
            filterChain.doFilter(req, resp)
            return
        }
        val token: String? = req.getHeader("Authorization")
            ?.takeIf { header -> header.startsWith("Bearer ") }
            ?.substring(7)
            ?: WebUtils.getCookie(req, TOKEN_COOKIE_KEY)
                ?.value

        if (token.isNullOrBlank().not()) {
            try {
                val claims: Claims = JWTHelper.validateToken(token) ?: throw Exception("invalid token")
                val username: String = claims.subject
                req.setAttribute("username", username)
                val userDetails: UserDetails = mockUserDetailService.loadUserByUsername(username)
                if (userDetails.username.equals(username, true).not())
                    throw Exception("invalid username")
                if (claims.expiration.before(Date()))
                    throw Exception("token expired")

                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(username, null, emptyList<SimpleGrantedAuthority>())
            } catch (e: Exception) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.message)
                return
            }
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Required")
            return
        }
        filterChain.doFilter(req, resp)
    }
}

@Configuration
@EnableWebSecurity
open class SecurityConfig {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

    @PostConstruct
    open fun init() {
        log.info("SecurityConfig initiating...")
    }

    @Bean(name = ["protectedResMatcher"])
    @Qualifier("protectedResMatcher")
    open fun apiRequestMatcher(): RequestMatcher = AntPathRequestMatcher("/api/**")

    @Bean
    open fun authRequestMatcher(): RequestMatcher = AntPathRequestMatcher("/authenticate")

    @Autowired
    open lateinit var mockUserDetailService: UserDetailsService

    @Throws(java.lang.Exception::class)
    @Bean
    open fun configure(http: HttpSecurity): SecurityFilterChain = http
        .csrf { customizer -> customizer.disable() }
        .addFilterBefore(JwtFilter(mockUserDetailService, apiRequestMatcher()), UsernamePasswordAuthenticationFilter::class.java)
        .userDetailsService(mockUserDetailService)
        .authorizeHttpRequests { customizer ->
            customizer
                .requestMatchers(authRequestMatcher())
                .permitAll()
                .requestMatchers(apiRequestMatcher())
                .authenticated()
        }.exceptionHandling(Customizer.withDefaults())
        .sessionManagement(Customizer.withDefaults())
        .build()
        .also { log.info(it.toString()) }

    @Bean
    open fun passwordEncoder(): PasswordEncoder = NoOpPasswordEncoder.getInstance()
}

@SpringBootApplication(scanBasePackages = [BASE_PACKAGE])
open class SecurityTestsApp {
    companion object {
        const val PORT: Int = 8082
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.spring.examples.security"
    }
}

//@ControllerAdvice
//open class ExceptionHandler : ApiExceptionHandler() {}

fun main(args: Array<String>) {
    SpringApplication.run(SecurityTestsApp::class.java,
                          *args,
                          "--server.port=$PORT",
                          "--server.compression.enabled=true",
                          "--server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml,application/x-javascript",
                          "--server.compression.min-response-size=2048")
}

open class SecurityTests {
}

