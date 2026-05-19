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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

object RepoTestSpringProps {
    const val ACTIVE_PROFILE_LAB: String = "spring.profiles.active=lab"
    const val DATASOURCE_URL: String =
        "spring.datasource.url=jdbc:h2:file:./${RepoTestDataDirs.TEMP_DIR_NAME}/${RepoTestDataDirs.H2_SUBDIR}/${RepoTestDataDirs.H2_DB_NAME};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    const val DATASOURCE_DRIVER: String = "spring.datasource.driver-class-name=org.h2.Driver"
    const val DATASOURCE_USERNAME: String = "spring.datasource.username=sa"
    const val DATASOURCE_PASSWORD: String = "spring.datasource.password="
    const val JPA_DDL_AUTO: String = "spring.jpa.hibernate.ddl-auto=update"
    const val JPA_SHOW_SQL: String = "spring.jpa.show-sql=true"
    const val JPA_OPEN_IN_VIEW: String = "spring.jpa.open-in-view=false"
    const val JPA_TIME_ZONE: String = "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
}

public object RepoTestDataDirs {
    const val TEMP_DIR_NAME: String = "temp"
    const val H2_SUBDIR: String = "h2"
    const val H2_DB_NAME: String = "repotests"

    fun projectRoot(): Path = Paths.get("").toAbsolutePath().normalize()

    fun tempRoot(): Path = projectRoot().resolve(TEMP_DIR_NAME)

    fun h2Directory(): Path = tempRoot().resolve(H2_SUBDIR)

    /** Creates `temp/h2` for H2 file storage (`repotests.mv.db`, etc.). */
    fun ensureH2Directory(): Path = h2Directory().also { Files.createDirectories(it) }
}