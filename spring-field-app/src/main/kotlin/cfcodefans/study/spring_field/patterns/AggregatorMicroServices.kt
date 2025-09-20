package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
//import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
//import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.io.IOException

object AggregatorMicroServices {
    data class Product(var title: String, var inventories: Int)

    @Component
    open class ProductInfoClient : () -> String? {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(ProductInfoClient::class.java)
        }

        @Autowired
        lateinit var restTempl: RestTemplate
        override fun invoke(): String? = try {
            restTempl
                .getForEntity<String>("http://localhost:51515/information")
                .body
        } catch (e: IOException) {
            log.error("IOException Occurred", e)
            null
        } catch (e: InterruptedException) {
            log.error("InterruptedException Occurred", e)
            null
        }
    }

    @Component
    open class ProductInventoryClient : () -> Int? {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(ProductInventoryClient::class.java)
        }

        @Autowired
        lateinit var restTempl: RestTemplate
        override fun invoke(): Int? = try {
            restTempl
                .getForEntity<String>("http://localhost:51516/inventories")
                .body
                ?.ifBlank { null }
                ?.toInt()
        } catch (e: IOException) {
            log.error("IOException Occurred", e)
            null
        } catch (e: InterruptedException) {
            log.error("InterruptedException  Occurred", e)
            null
        }
    }

    @RestController
    open class Aggregator {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(Aggregator::class.java)
        }

        @Autowired
        lateinit var infoClient: () -> String?

        @Autowired
        lateinit var inventoryClient: () -> Int?

        @GetMapping("/product")
        open fun getProduct(): Product = Product(
                title = infoClient() ?: "Error: Fetching Product Title Failed",
                inventories = inventoryClient() ?: -1)
    }

    @SpringBootApplication
    @EnableAutoConfiguration
//    @EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
    open class AggregatorMicroServicesApp {
        @Bean
        open fun restTempl(): RestTemplate = RestTemplate()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(AggregatorMicroServicesApp::class.java,
                    ProductInfoClient::class.java,
                    ProductInventoryClient::class.java,
                    Aggregator::class.java,
                    RestTemplate::class.java)
            .properties(mapOf("server.port" to "50000"))
            .run()
    }
}

object InfoServices {
    @RestController
    open class InfoCtrl {

        companion object {
            private val log: Logger = LoggerFactory.getLogger(InfoCtrl::class.java)
        }

        @GetMapping("/information")
        open fun getProductTitle(): String = "The Product Title."
    }

    @SpringBootApplication
    @EnableAutoConfiguration
//    @EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
    open class InfoServicesApp {}

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(InfoServicesApp::class.java,
                    InfoCtrl::class.java)
            .properties(mapOf("server.port" to "51515")) //
            .run()
    }
}

object InventoryServices {
    @RestController
    open class InventoryCtrl {

        companion object {
            private val log: Logger = LoggerFactory.getLogger(InventoryServices::class.java)
        }

        @GetMapping("/inventories")
        open fun getProductInventories(): Int = 5
    }

    @SpringBootApplication
    @EnableAutoConfiguration//(exclude = [GsonAutoConfiguration::class])
    open class InventoryServicesApp {}

    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder()
            .web(WebApplicationType.SERVLET)
            .sources(InventoryServicesApp::class.java,
                    InventoryCtrl::class.java)
            .properties(mapOf("server.port" to "51516")) //
            .run()
    }
}