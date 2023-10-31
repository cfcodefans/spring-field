package cfcodefans.study.spring_field.quoters

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import java.util.*
import kotlin.random.Random


@Configuration
open class DatabaseLoader {
    @Bean
    open fun init(quoterRepo: QuoteRepo): CommandLineRunner {
        return CommandLineRunner { args ->
            quoterRepo.save(Quote(quote = "Working with Spring Boot is like pair-programming with the Spring developers."))
            quoterRepo.save(Quote(quote = "With Boot you deploy everywhere you can find a JVM basically."))
            quoterRepo.save(Quote(quote = "Spring has come quite a ways in addressing developer enjoyment and ease of use since the last time I built an application using it."))
            quoterRepo.save(Quote(quote =
            "Previous to Spring Boot, I remember XML hell, confusing set up, and many hours of frustration."))
            quoterRepo.save(Quote(quote = "Spring Boot solves this problem. It gets rid of XML and wires up common components for me, so I don't have to spend hours scratching my head just to figure out how it's all pieced together."))
            quoterRepo.save(Quote(quote = "It embraces convention over configuration, providing an experience on par with frameworks that excel at early stage development, such as Ruby on Rails."))
            quoterRepo.save(Quote(quote = "The real benefit of Boot, however, is that it's just Spring. That means any direction the code takes, regardless of complexity, I know it's a safe bet."))
            quoterRepo.save(Quote(quote = "I don't worry about my code scaling. Boot allows the developer to peel back the layers and customize when it's appropriate while keeping the conventions that just work."))
            quoterRepo.save(Quote(quote = "So easy it is to switch container in #springboot."))
            quoterRepo.save(Quote(quote = "Really loving Spring Boot, makes stand alone Spring apps easy."))
            quoterRepo.save(Quote(quote = "I have two hours today to build an app from scratch. @springboot to the rescue!"))
            quoterRepo.save(Quote(quote = "@springboot with @springframework is pure productivity! Who said in #java one has to write double the code than in other langs? #FavLib"))
        }
    }
}

@Entity
open class Quote(@Id @GeneratedValue open var id: Long? = null,
                 open var quote: String? = null) {

    override fun equals(other: Any?): Boolean = other is Quote
            && other.id == this.id
            && other.quote == this.quote

    override fun hashCode(): Int = Objects.hash(Quote::class.java, id, quote)
    override fun toString(): String = "Quote{id=$id, quote='$quote'}"
}

@RestController
open class QuoteCtrl {

    companion object {
        val NONE: Quote = Quote(quote = "None")
        fun nextLong(range: LongRange): Long = range.random(Random.Default)
    }

    @Autowired
    lateinit var quoteRepo: QuoteRepo

    @GetMapping("/api")
    open fun getAll(): List<QuoteResource> = quoteRepo.findAll().map { QuoteResource(it, "success") }

    @GetMapping("/api/{id}")
    open fun getOne(@PathVariable("id") id: Long?): QuoteResource = quoteRepo
        .findByIdOrNull(id)
        ?.let { QuoteResource(it, "success") }
        ?: QuoteResource(NONE, "Quote $id does not exist")

    @GetMapping("/api/random")
    open fun getRandomOne(): QuoteResource = getOne(nextLong(1L..quoteRepo.count() + 1))
}

@Repository
interface QuoteRepo : JpaRepository<Quote, Long>

data class QuoteResource(val quote: Quote, val type: String)

@SpringBootApplication
@EnableAutoConfiguration
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = false)
@EnableScheduling
open class QuotersIncorporatedApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(QuotersIncorporatedApp::class.java)
    }
}

@Configuration
open class SwaggerCfg {
    @Bean(name = ["swagger"])
    open fun createRestApi(): Docket = Docket(DocumentationType.OAS_30)
        .forCodeGeneration(true)
        .protocols(setOf("http"))
        .apiInfo(ApiInfoBuilder()
            .title("spring-field-app")
            .description("spring-field-app")
            .version("0.0.1")
            .build())
        //        .apply {
        //            val tr = TypeResolver()
        //            AdminConstants.ADMIN_API_MODEL_CLZZ.map { tr.resolve(it) }.forEach { this.additionalModels(it) }
        //        }
        .select()
        //        .apis(RequestHandlerSelectors.basePackage("${Constants.BASE_PACKAGE}.admin.ctrl")
        //                  .or(RequestHandlerSelectors.basePackage("${Constants.BASE_PACKAGE}.common.ctrl"))
        //                  .or(RequestHandlerSelectors.basePackage("${Constants.BASE_PACKAGE}.coingate.ctrl")))
        .paths(PathSelectors.any())
        .build()
}

fun main(args: Array<String>) {
    SpringApplication.run(QuotersIncorporatedApp::class.java, *args)
}