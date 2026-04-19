package cfcodefans.study.spring_field

import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import kotlin.reflect.KClass

object Constants {

}

object TestContextProfiles {
    /**
     * Excludes all web-related configurations. Use this for tests that
     * only interact with services, repositories, or other backend logic.
     */
    val NO_WEB_CONTEXT: Array<KClass<out Any>> = arrayOf(
//            ServletWebServerFactoryAutoConfiguration::class,
            DispatcherServletAutoConfiguration::class,
            WebMvcAutoConfiguration::class,
            ErrorMvcAutoConfiguration::class,
            // Security and SpringDoc are often web-dependent
            SecurityAutoConfiguration::class,
            UserDetailsServiceAutoConfiguration::class,
            SpringDocConfiguration::class,
            SpringDocWebMvcConfiguration::class,
            // Actuator web endpoints
//            ManagementWebserverAutoConfiguration::class,
//            EndpointAutoConfiguration::class
    )

    /**
     * Excludes all data and persistence-related configurations.
     * Use this for tests that focus on web controllers or utility classes
     * without hitting a database.
     */
    val NO_JPA_CONTEXT: Array<KClass<out Any>> = arrayOf(
            DataSourceAutoConfiguration::class,
            HibernateJpaAutoConfiguration::class,
            DataSourceTransactionManagerAutoConfiguration::class)

    /**
     * A truly minimal context for simple utility or configuration tests.
     * It combines the other profiles to form a fast, lightweight context.
     */
    val MINIMAL_CONTEXT: Array<KClass<out Any>> = NO_WEB_CONTEXT + NO_JPA_CONTEXT + arrayOf(
            // Add any other specific exclusions for the minimal profile
//            GsonAutoConfiguration::class
            JmxAutoConfiguration::class)
}