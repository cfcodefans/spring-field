package cfcodefans.study.spring_field.standalone_web.template

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OpenAPIDefinition()
@RestController
@RequestMapping("/api")
open class BizCtrl {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BizCtrl::class.java)
        val DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @Operation(method = "GET", summary = "get current time")
    @GetMapping("/now")
    open fun getCurrentDateTime(): String = LocalDateTime.now().format(DATETIME_FMT)
}