package cfcodefans.study.junit

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime


@Suite
@SuiteDisplayName("Test Suite for Demo Project")
@SelectClasses(DummyServiceTestsA::class, DummyServiceTestsB::class)
open class SpringBootTestSuite {
}

@Service("dummy")
@Qualifier("dummy")
open class DummyService : (Any) -> String {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DummyService::class.java)
    }

    @PostConstruct
    open fun init() {
        log.info("DummyService.init...")
    }

    override fun invoke(p1: Any): String = p1.toString()
}

@SpringBootApplication(
        scanBasePackages = ["cfcodefans.study.junit"],
        exclude = [
            ServletWebServerFactoryAutoConfiguration::class,
            DispatcherServletAutoConfiguration::class,
            WebMvcAutoConfiguration::class,
            ErrorMvcAutoConfiguration::class,
            // Security and SpringDoc are often web-dependent
            SecurityAutoConfiguration::class,
            UserDetailsServiceAutoConfiguration::class,
            SpringDocConfiguration::class,
            SpringDocWebMvcConfiguration::class,

            DataSourceAutoConfiguration::class,
            HibernateJpaAutoConfiguration::class,
            DataSourceTransactionManagerAutoConfiguration::class,

            JmxAutoConfiguration::class,
            GsonAutoConfiguration::class
        ])
@EnableAspectJAutoProxy(proxyTargetClass = false)
open class DummySpringBootApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DummySpringBootApp::class.java)
    }
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [DummySpringBootApp::class],
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                useMainMethod = UseMainMethod.NEVER,
        // Add this line to ensure no profiles are activated for this test.
                properties = ["spring.profiles.active="])
open class DummyServiceTestBase {}

open class DummyServiceTestsA : DummyServiceTestBase() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DummyServiceTestsA::class.java)
    }

    @Autowired(required = true)
    @Qualifier("dummy")
    open lateinit var dummy: (Any) -> String

    @Autowired
    open lateinit var appCxt: AnnotationConfigApplicationContext

    @Test
    open fun callDummy() {
        Assertions.assertNotNull(dummy)
        Assertions.assertNotNull(appCxt)
        Assertions.assertTrue(dummy(LocalDateTime.now()).also { log.info(it) }.isBlank().not())
    }

    @Test
    open fun inspectApp() {
        Assertions.assertNotNull(dummy)
        Assertions.assertNotNull(appCxt)
        log.info(appCxt.toString())

        appCxt.beanDefinitionNames
            .map { name -> appCxt.getBeanDefinition(name) }
            .filter { b -> b.beanClassName?.contains("cfcodefans") == true }
            .map { bd -> "${bd.factoryBeanName}\t${bd.beanClassName}" }
            .sorted()
            .joinToString(separator = "\n\r")
            .let { log.info(it) }
    }
}

open class DummyServiceTestsB : DummyServiceTestBase() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(DummyServiceTestsB::class.java)
    }

    @Autowired(required = true)
    @Qualifier("dummy")
    open lateinit var dummy: (Any) -> String

    @Autowired
    open lateinit var appCxt: AnnotationConfigApplicationContext

    @Test
    open fun callDummy() {
        Assertions.assertNotNull(dummy)
        Assertions.assertNotNull(appCxt)
        Assertions.assertTrue(dummy("test B").also { log.info(it) }.isBlank().not())
    }

    @Test
    open fun inspectApp() {
        Assertions.assertNotNull(dummy)
        Assertions.assertNotNull(appCxt)
        log.info(appCxt.toString())

        appCxt.beanDefinitionNames
            .map { name -> appCxt.getBeanDefinition(name) }
            .filter { b -> b.beanClassName?.contains("cfcodefans") == true }
            .map { bd -> "${bd.factoryBeanName}\t${bd.beanClassName}" }
            .sorted()
            .joinToString(separator = "\n\r")
            .let { log.info(it) }
    }
}