package cfcodefans.study.spring_field.spring.boot

//import com.turkraft.springfilter.boot.PageSortAutoConfiguration
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.data.autoconfigure.metrics.DataRepositoryMetricsAutoConfiguration
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.graphql.autoconfigure.security.GraphQlWebMvcSecurityAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.metrics.HibernateMetricsAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import java.lang.annotation.Inherited

/**
 * Composed `@EnableAutoConfiguration` markers with fixed `exclude` lists. Scope is only
 * auto-configuration; pair with `@SpringBootConfiguration` and `@ComponentScan` (or other
 * annotations) on each application class as needed.
 *
 * Do **not** use these together with `@SpringBootApplication` on the same type (that would apply
 * two `@EnableAutoConfiguration` declarations). Prefer `@SpringBootApplication(exclude = …)` for
 * full apps; use this split when you want small, composable building blocks.
 *
 * Exclusion sets mirror `cfcodefans.study.spring_field.TestContextProfiles` where noted; keep them
 * in sync when those profiles change.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@EnableAutoConfiguration(exclude = [
    SecurityAutoConfiguration::class,
    UserDetailsServiceAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
    GraphQlWebMvcSecurityAutoConfiguration::class,
])
annotation class AutoCfgWithoutSecurity

/** Same exclusions as [cfcodefans.study.spring_field.TestContextProfiles.NO_JPA_CONTEXT]. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@EnableAutoConfiguration(exclude = [
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataWebAutoConfiguration::class,
    DataRepositoryMetricsAutoConfiguration::class,
    DataJpaRepositoriesAutoConfiguration::class,
    HibernateMetricsAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,

    TransactionAutoConfiguration::class,
    JtaAutoConfiguration::class,

    //graph ql related
    GraphQlAutoConfiguration::class,

//    PageSortAutoConfiguration::class
])
//@EnableJpaRepositories(basePackages = [], bootstrapMode = BootstrapMode.DEFERRED)
annotation class AutoCfgWithoutDataJpa


/**
 * Same exclusions as [cfcodefans.study.spring_field.TestContextProfiles.NO_WEB_CONTEXT]
 * (MVC, error page, SpringDoc, security). For headless slices that should not start the servlet web stack.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@EnableAutoConfiguration(exclude = [
    DispatcherServletAutoConfiguration::class,
    WebMvcAutoConfiguration::class,
    ErrorMvcAutoConfiguration::class,
    SecurityAutoConfiguration::class,
    UserDetailsServiceAutoConfiguration::class,
    SpringDocConfiguration::class,
    SpringDocWebMvcConfiguration::class,
])
annotation class AutoCfgWithoutServletWebStack

/** [AutoCfgWithoutServletWebStack] plus [AutoCfgWithoutDataJpa]. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@EnableAutoConfiguration(exclude = [
    DispatcherServletAutoConfiguration::class,
    WebMvcAutoConfiguration::class,
    ErrorMvcAutoConfiguration::class,
    SecurityAutoConfiguration::class,
    UserDetailsServiceAutoConfiguration::class,
    SpringDocConfiguration::class,
    SpringDocWebMvcConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
])
annotation class AutoCfgWithoutServletWebStackAndDataJpa

/** Same exclusions as [cfcodefans.study.spring_field.TestContextProfiles.MINIMAL_CONTEXT]. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@EnableAutoConfiguration(exclude = [
    DispatcherServletAutoConfiguration::class,
    WebMvcAutoConfiguration::class,
    ErrorMvcAutoConfiguration::class,
    SecurityAutoConfiguration::class,
    UserDetailsServiceAutoConfiguration::class,
    SpringDocConfiguration::class,
    SpringDocWebMvcConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
    JmxAutoConfiguration::class
])
annotation class AutoCfgMinimalTestContext
