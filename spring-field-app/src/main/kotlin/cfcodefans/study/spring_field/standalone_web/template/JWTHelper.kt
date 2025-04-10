package cfcodefans.study.spring_field.standalone_web.template

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.IOException
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.WebUtils
import java.util.*


object JWTHelper {
    private val SECRET_KEY: String = "This-is-a-very-long-and-complicated-secret".let { Base64.getEncoder().encodeToString(it.toByteArray()) } // Replace with a strong secret key
    private const val EXPIRATION_TIME: Long = 86400000 // 1 day
    val jwtParser: JwtParser = Jwts
        .parser()
        .verifyWith(SECRET_KEY
            .let { Decoders.BASE64.decode(it) }
            .let { Keys.hmacShaKeyFor(it) })
        .build()

    fun generateToken(username: String?): String? = Jwts.builder()
        .subject(username)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(Keys.hmacShaKeyFor(SECRET_KEY.let { Decoders.BASE64.decode(it) }), SignatureAlgorithm.HS256)
        .compact()

    fun validateToken(token: String?): Claims? = jwtParser
        .parseSignedClaims(token)
        .payload
        .takeIf { it is Claims }
        .let { it as Claims }
}

open class JwtFilter(private val reqMatcher: RequestMatcher) : OncePerRequestFilter() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(JwtFilter::class.java)
        const val TOKEN_COOKIE_KEY: String = "jwtToken"
    }

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
                JWTHelper.validateToken(token)
                    ?.subject
                    ?.also { username -> req.setAttribute("username", username) }
                    ?.also { username ->
                        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(username, null, emptyList<SimpleGrantedAuthority>())
                    }
            } catch (e: Exception) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token")
                return
            }
        } else {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Required")
            return
        }
        filterChain.doFilter(req, resp)
    }

}