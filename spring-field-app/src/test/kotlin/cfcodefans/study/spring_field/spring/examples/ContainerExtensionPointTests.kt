package cfcodefans.study.spring_field.spring.examples.container_exts

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

open class BeanPostProc : BeanPostProcessor {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BeanPostProc::class.java)
    }

    init {
        log.info("BeanPostProc.init...")
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        log.info("BeanPostProc.postProcessAfterInitialization($beanName)")
        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        log.info("BeanPostProc.postProcessBeforeInitialization($beanName)")
        return super.postProcessBeforeInitialization(bean, beanName)
    }
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

    override fun invoke(p1: Any): String {
        return p1.toString()
    }
}

@SpringBootApplication(scanBasePackages = ["cfcodefans.study.spring_field.spring.examples.container_exts"])
@EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class])
@EnableAspectJAutoProxy(proxyTargetClass = false)
open class ContainerExtApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ContainerExtApp::class.java)
    }

    @Bean
    open fun beanPostProc(): BeanPostProc = BeanPostProc()
}

//fun main(args: Array<String>) {
//    SpringApplication.run(ContainerExtApp::class.java, *args)
//}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ContainerExtApp::class],
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        useMainMethod = UseMainMethod.WHEN_AVAILABLE)
open class ContainerExtensionPointTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ContainerExtensionPointTests::class.java)
    }

    @Autowired(required = true)
    @Qualifier("dummy")
    open lateinit var dummy: (Any) -> String

    @Autowired
    open lateinit var appCxt: AnnotationConfigApplicationContext

    @Test
    open fun callDummy() {
        Assertions.assertTrue(dummy(LocalDateTime.now()).isBlank().not())
    }

    @Test
    open fun inspectApp() {
        log.info(appCxt::class.qualifiedName)

        appCxt.beanDefinitionNames
            .map { name -> appCxt.getBeanDefinition(name) }
            .filter { b -> b.beanClassName?.contains("cfcodefans") == true }
            .map { bd -> "${bd.factoryBeanName}\t${bd.beanClassName}" }
            .sorted()
            .joinToString(separator = "\n\r")
            .let { log.info(it) }
    }
}