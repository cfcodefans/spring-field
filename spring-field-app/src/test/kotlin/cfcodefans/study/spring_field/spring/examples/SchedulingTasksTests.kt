package cfcodefans.study.spring_field.spring.examples.schedule

import com.fasterxml.jackson.databind.util.StdDateFormat
import jakarta.annotation.PostConstruct
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.verify
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
//import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.text.DateFormat
import java.util.*


@Component
open class ScheduledTasks {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ScheduledTasks::class.java)
        val DATE_FMT: DateFormat = StdDateFormat()
    }

    @Scheduled(fixedRate = 5000)
    open fun reportCurrentTime() {
        log.info("The time is now ${DATE_FMT.format(Date())}")
    }

    @PostConstruct
    open fun init() {
        log.info("${this.javaClass.simpleName} initiating...")
    }
}

@SpringBootApplication(scanBasePackages = ["cfcodefans.study.spring_field.spring.examples.schedule"])
@EnableAutoConfiguration(exclude = [//GsonAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
@EnableScheduling
open class SchedulingTasksApp {

}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SchedulingTasksApp::class],
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                properties = ["spring.profiles.active="],
                useMainMethod = UseMainMethod.WHEN_AVAILABLE)
open class SchedulingTasksTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SchedulingTasksTests::class.java)
    }

    @Autowired
    lateinit var tasksRef: ScheduledTasks

    @Test
    open fun contextLoads() {
        // Basic integration test that shows the context starts up properly
        Assertions.assertNotNull(tasksRef)
    }

    @MockitoSpyBean
    lateinit var tasks: ScheduledTasks

    @Test
    open fun testSpyBean() {
        log.info("""
            tasks: $tasks
            tasks: $tasksRef
            are the same: ${tasks === tasksRef}""".trimIndent())
    }


    @Test
    open fun reportCurrentTime() {
        await()
            .atMost(Durations.TEN_SECONDS)
            .untilAsserted { verify(tasksRef, atLeast(2)).reportCurrentTime() }
    }
}