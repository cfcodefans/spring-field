package cfcodefans.study.spring_field.graphql.spqr

import cfcodefans.study.spring_field.graphql.spqr.GraphSpqrWebApp.Companion.BASE_PACKAGE
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * SPQR sample app: self-contained under [BASE_PACKAGE] (duplicated JPA model vs `graphql.standard`).
 */
@SpringBootApplication(
    scanBasePackages = [GraphSpqrWebApp.BASE_PACKAGE],
    exclude = [
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        GraphQlAutoConfiguration::class,
        GraphQlWebMvcAutoConfiguration::class,
    ],
)
@EnableJpaAuditing
@EntityScan(basePackageClasses = [GraphEntity::class])
@EnableJpaRepositories(basePackageClasses = [GraphEntityRepo::class])
open class GraphSpqrWebApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphSpqrWebApp::class.java)
        const val BASE_PACKAGE: String = "cfcodefans.study.spring_field.graphql.spqr"
        const val PORT: Int = 8083
    }

    @PostConstruct
    open fun logStartupHints() {
        log.info(
            """GraphSpqrWebApp — SPQR runtime schema; POST JSON to http://localhost:${PORT}/graphql ;
            |GET SDL at http://localhost:${PORT}/graphql/schema (profile graphql-spqr-web).""".trimMargin(),
        )
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(
        GraphSpqrWebApp::class.java,
        *args,
        "--spring.profiles.active=graphql-spqr-web",
        "--server.port=${GraphSpqrWebApp.PORT}",
        "--server.compression.enabled=true",
    )
}
