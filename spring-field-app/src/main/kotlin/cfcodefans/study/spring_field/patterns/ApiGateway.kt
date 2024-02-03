package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/api-gateway/
 */
object ApiGateway {
    data class DesktopProduct(var price: String?, var imagePath: String?)

    data class MobileProduct(var price: String?)

    fun isOk(respCode: Int): Boolean = respCode in 200..299

    interface IPriceClient {
        fun getPrice(): String?
    }

    @Component
    open class PriceClient : IPriceClient {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(PriceClient::class.java)
        }

        private fun logResp(resp: HttpResponse<String>) = if (isOk(resp.statusCode()))
            log.info("Price info received successfully")
        else log.warn("Price info request failed")

        override fun getPrice(): String? = runCatching {
            log.info("Sending request to fetch price info")
            HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("http://localhost:50006/price"))
                    .build(),
                        HttpResponse.BodyHandlers.ofString())
                .also { logResp(it) }
                .body()
        }.onFailure { e ->
            log.error("Failure occurred while getting price info", e)
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.getOrNull()
    }

    interface IImageClient {
        fun getImagePath(): String?
    }

    @Component
    open class ImageClient : IImageClient {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(ImageClient::class.java)
        }

        private fun logResp(resp: HttpResponse<String>) = if (isOk(resp.statusCode()))
            log.info("Image path received successfully")
        else log.warn("Image path request failed")

        override fun getImagePath(): String? = runCatching {
            log.info("Sending request to fetch image path")
            HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("http://localhost:50005/image-path"))
                    .build(),
                        HttpResponse.BodyHandlers.ofString())
                .also { logResp(it) }
                .body()
        }.onFailure { e ->
            log.error("Failure occurred while getting image path", e)
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.getOrNull()
    }

    @RestController
    open class ApiGateway {
        @Autowired
        lateinit var imgClient: IImageClient

        @Autowired
        lateinit var priceClient: IPriceClient

        @GetMapping("/desktop")
        open fun getProductDesktop(): DesktopProduct = DesktopProduct(
                price = priceClient.getPrice(),
                imagePath = imgClient.getImagePath())

        @GetMapping("/mobile")
        open fun getProductMobile(): MobileProduct = MobileProduct(price = priceClient.getPrice())
    }

    @SpringBootApplication
    @EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
    open class ApiGatewayApp {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(ApiGatewayApp::class.java,
                    ApiGateway::class.java,
                    ImageClient::class.java,
                    PriceClient::class.java,
                    RestTemplate::class.java)
            .properties(mapOf("server.port" to "50000"))
            .run()
    }
}

object ImageServices {
    @RestController
    open class ImageCtrl {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(ImageCtrl::class.java)
        }

        @GetMapping("/image-path")
        open fun getImgPath(): String {
            log.info("Successfully found image path")
            return "/product-image.png"
        }
    }

    @SpringBootApplication
    @EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
    open class ImageApp {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(ImageApp::class.java,
                    ImageCtrl::class.java)
            .properties(mapOf("server.port" to "50005"))
            .run()
    }
}

object PriceServices {
    @RestController
    open class PriceCtrl {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(PriceCtrl::class.java)
        }

        @GetMapping("/price")
        open fun getPrice(): String {
            log.info("Successfully found price info")
            return "20"
        }
    }

    @SpringBootApplication
    @EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
    open class PriceApp {
    }

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(PriceApp::class.java,
                    PriceCtrl::class.java)
            .properties(mapOf("server.port" to "50006"))
            .run()
    }
}