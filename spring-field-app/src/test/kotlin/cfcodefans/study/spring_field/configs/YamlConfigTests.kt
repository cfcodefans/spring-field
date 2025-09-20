package cfcodefans.study.spring_field.configs

import cfcodefans.study.spring_field.commons.Jsons
import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension


data class RepoCfg @ConstructorBinding constructor(val url: String,
                                                   val username: String,
                                                   val password: String,
                                                   val repo: String,
                                                   val branches: Array<String>)

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@PropertySource(value = ["classpath:/cfcodefans/study/spring_field/configs/test-cfgs.yml"])
open class Configs {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Configs::class.java)
    }

//    @Value("\${fileExts}")
    var fileExts: List<String> = emptyList()
        @ConfigurationProperties(prefix = "fileExts")
        set

    @Value("\${testAppName}")
    lateinit var appName: String

    //    @Value("\${repos}")
    var siteToRepoCfgs: Map<String, RepoCfg> = emptyMap()
        @ConfigurationProperties(prefix = "repos")
        set

    init {
        log.info("${Configs.javaClass} init")
    }

    @PostConstruct
    fun init() {
        log.info("${Configs.javaClass}.init()")
    }

    override fun toString(): String = Jsons.toString(mapOf(
            "appName" to appName,
            "fileExts" to fileExts,
            "repos" to siteToRepoCfgs
    ))
}

@SpringBootApplication
@EnableAutoConfiguration(exclude = [//GsonAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
open class DummyYamlApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DummyYamlApp::class.java)
    }
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [DummyYamlApp::class],
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        useMainMethod = UseMainMethod.WHEN_AVAILABLE)
open class YamlConfigTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(YamlConfigTests::class.java)
    }

    @Autowired
    lateinit var configs: Configs

    @Test
    open fun testCfgs() {
        log.info("cfgs: ${configs}")
    }
}