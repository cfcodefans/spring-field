package cfcodefans.study.spring_field.standalone_web.template

import cfcodefans.study.spring_field.commons.Jsons
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.swagger.v3.oas.annotations.parameters.RequestBody as ReqBody

@Schema(name = "DummyForm")
data class DummyForm(@Parameter(required = true, name = "username") val username: String,
                     @Parameter(required = true, name = "password") val password: String,
                     @Hidden val createdAt: LocalDateTime = LocalDateTime.now()) {
    override fun toString(): String = Jsons.toString(this)
}

@Schema(name = "DummyForm2")
data class DummyForm2(@Parameter(required = true, name = "param_msg")
                      @Schema(name = "message", defaultValue = "default message by schema")
                      val msg: String = "default message",
                      @Parameter(required = true, name = "param_sender_uid")
                      @Schema(name = "sender_uid", minimum = "0", defaultValue = "0")
                      val senderUid: Long = 0,
                      @Hidden val createdAt: LocalDateTime = LocalDateTime.now()) {
    override fun toString(): String = Jsons.toString(this)
}

@OpenAPIDefinition()
@RestController
@RequestMapping("/public")
open class PublicResCtrl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(PublicResCtrl::class.java)
        val DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @Operation(method = "GET", summary = "get current time")
    @GetMapping("/now")
    open fun getCurrentDateTime(): String = LocalDateTime.now().format(BizCtrl.Companion.DATETIME_FMT)

    @Operation(method = "POST",
               summary = "dummy form",
               requestBody = ReqBody(content = [Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)]))
    @PostMapping(path = ["/dummy-form"],
                 consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun echoForm(dummyForm: DummyForm): String = dummyForm.toString()

    @Operation(method = "POST",
               summary = "dummy form",
               requestBody = ReqBody(content = [Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)]))
    @PostMapping(path = ["/dummy-form-2"],
                 consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun echoForm2(dummyForm: DummyForm2): String = dummyForm.toString()

    @Operation(method = "POST",
               summary = "dummy form",
               requestBody = ReqBody(content = [
                   Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE, schema = Schema(implementation = DummyForm2::class))
               ]))
    @PostMapping(path = ["/dummy-form-3"],
                 consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun echoForm3(req: HttpServletRequest): String = DummyForm2(
            msg = req.parameterMap["message"]?.firstOrNull() ?: "default message by request",
            senderUid = req.parameterMap["sender_uid"]?.firstOrNull()?.toLong() ?: -1
    ).toString()
}