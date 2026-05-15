package cfcodefans.study.spring_field.commons

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

open class TrafficLogFilter : OncePerRequestFilter() {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(TrafficLogFilter::class.java)
        fun isPrintable(mediaType: MediaType?): Boolean = when (mediaType) {
            null -> false
            else -> mediaType.isCompatibleWith(MediaType.TEXT_PLAIN)
                    || mediaType.isCompatibleWith(MediaType.TEXT_XML)
                    || mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                    || mediaType.isCompatibleWith(MediaType.TEXT_HTML)
        }
    }

    override fun doFilterInternal(req: HttpServletRequest, resp: HttpServletResponse, chain: FilterChain) {
        val reqWrapper: ContentCachingRequestWrapper = ContentCachingRequestWrapper(req, 0)
        val respWrapper: ContentCachingResponseWrapper = ContentCachingResponseWrapper(resp)

        val start: Long = System.currentTimeMillis()
        try {
            chain.doFilter(reqWrapper, respWrapper)
        } finally {
            logReqAndResp(reqWrapper = reqWrapper, respWrapper = respWrapper, start = start)
            respWrapper.copyBodyToResponse()
        }
    }

    private fun logReqAndResp(reqWrapper: ContentCachingRequestWrapper,
                              respWrapper: ContentCachingResponseWrapper,
                              start: Long): Unit = try {
        val reqBody: String = String(reqWrapper.contentAsByteArray)
        val respContentType: MediaType? = respWrapper.contentType
            ?.takeIf { it.isBlank().not() }
            ?.let { MediaType.parseMediaType(it) }
        val respContent: String = String(respWrapper.contentAsByteArray)
        log.info("""
                            request:        ${reqWrapper.remoteAddr}
                            url:            ${reqWrapper.requestURL}
                            method:         ${reqWrapper.method}
                            header:         ${reqWrapper.headerNames.toList().associateWith { hn -> reqWrapper.getHeader(hn) }.let { Jsons3.toJson(it) }}
                            queryString:    ${reqWrapper.queryString}
                            params:         ${Jsons3.fakeJson(reqWrapper.parameterMap)}
                            content:    $reqBody
                            
                            took        ${System.currentTimeMillis() - start} ms to get
                            
                            response:
                            header:         ${respWrapper.headerNames.toList().associateWith { hn -> respWrapper.getHeader(hn) }.let { Jsons3.toJson(it) }}
                            code:           ${respWrapper.status}
                            content:        ${
            if (respContentType == null || isPrintable(respContentType)) respContent else "can not print"
        }""".trimIndent())

        Unit
    } catch (e: Exception) {
        log.error("failed to log the request", e)
    } finally {
        Unit
    }
}