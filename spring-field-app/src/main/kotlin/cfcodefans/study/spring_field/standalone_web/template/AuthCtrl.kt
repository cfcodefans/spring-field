package cfcodefans.study.spring_field.standalone_web.template

import cfcodefans.study.spring_field.standalone_web.template.JwtFilter.Companion.TOKEN_COOKIE_KEY
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
open class AuthCtrl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(AuthCtrl::class.java)
    }

    @PostMapping(path = ["/login"],
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
            produces = [MediaType.TEXT_PLAIN_VALUE])
    open fun login(@RequestParam(name = "username") username: String,
                   @RequestParam(name = "pwd") pwd: String,
                   resp: HttpServletResponse): String = JWTHelper
        .generateToken(username)
        ?.also { token -> resp.setHeader(HttpHeaders.SET_COOKIE, "$TOKEN_COOKIE_KEY=$token; Path=/; HttpOnly; Secure; SameSite=Strict") }
        ?.let { "ok" }
        ?: throw IllegalAccessException("invalid username or password")
}