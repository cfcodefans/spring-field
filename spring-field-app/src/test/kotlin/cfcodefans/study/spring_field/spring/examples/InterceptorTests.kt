package cfcodefans.study.spring_field.spring.examples.interceptors

import cfcodefans.study.spring_field.spring.examples.ContainerExtApp2
import jakarta.annotation.PostConstruct
import org.aopalliance.intercept.MethodInvocation
import org.apache.commons.logging.Log
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AbstractTraceInterceptor
import org.springframework.aop.interceptor.CustomizableTraceInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import java.time.LocalDateTime

/**
 * refers to https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/example.html
 */
open class MethodInvocationAdapter(private val pjp: ProceedingJoinPoint) : MethodInvocation {
    override fun getMethod(): Method = (pjp.signature as MethodSignature).method

    override fun getArguments(): Array<out Any?> = pjp.args

    override fun proceed(): Any? = pjp.proceed()

    override fun getThis(): Any? = pjp.`this`

    override fun getStaticPart(): AccessibleObject = pjp.staticPart as AccessibleObject
}

@Aspect
@Component
open class TracingAspect {
    val traceInterceptor: AbstractTraceInterceptor = object : CustomizableTraceInterceptor() {
        init {
            this.setEnterMessage("\n${PLACEHOLDER_TARGET_CLASS_SHORT_NAME}.${PLACEHOLDER_METHOD_NAME}(${PLACEHOLDER_ARGUMENTS}) { \n")
            this.setExitMessage("\n} $PLACEHOLDER_RETURN_VALUE \n")
            this.setLogExceptionStackTrace(true)
            this.setExceptionMessage("Oops")
        }

        override fun isLogEnabled(logger: Log): Boolean = true
        override fun writeToLog(logger: Log, message: String) = logger.info(message)
        override fun writeToLog(logger: Log, message: String, ex: Throwable?) = ex?.let { logger.error(message, ex) } ?: logger.info(message)
    }

    @Pointcut("within(cfcodefans.study.spring_field.spring.examples.interceptors.*)")
    open fun beanMethods(): Unit = Unit

    @Throws(Throwable::class)
    @Around("cfcodefans.study.spring_field.spring.examples.interceptors.TracingAspect.beanMethods()")
    open fun trace(pjp: ProceedingJoinPoint): Any? = traceInterceptor.invoke(MethodInvocationAdapter(pjp))

//    @Bean
//    open fun tracingAspect(): TracingAspect = TracingAspect()
}

@Service("foo")
@Qualifier("foo")
open class FooService : (Any) -> String {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BarService::class.java)
    }

    @PostConstruct
    open fun init() {
        log.info("FooService.init...")
    }

    override fun invoke(p1: Any): String = p1.toString()
}

@Service("bar")
@Qualifier("bar")
open class BarService : (Any) -> String {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BarService::class.java)
    }

    @Autowired
    @Qualifier("foo")
    lateinit var foo: (Any) -> String

    @PostConstruct
    open fun init() {
        log.info("BarService.init...")
    }

    override fun invoke(p1: Any): String = "foo said: ${foo(p1)}"
}

@SpringBootApplication(scanBasePackages = ["cfcodefans.study.spring_field.spring.examples.interceptors"])
@EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
@EnableAspectJAutoProxy(proxyTargetClass = false)
open class ContainerExtApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ContainerExtApp2::class.java)
    }

}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ContainerExtApp2::class],
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                useMainMethod = UseMainMethod.WHEN_AVAILABLE)
open class InterceptorTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(InterceptorTests::class.java)
    }

    @Autowired(required = true)
    @Qualifier("bar")
    open lateinit var bar: (Any) -> String

    @Autowired
    open lateinit var appCxt: AnnotationConfigApplicationContext

    @Test
    open fun callBar() {
        Assertions.assertTrue(bar(LocalDateTime.now()).isBlank().not())
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