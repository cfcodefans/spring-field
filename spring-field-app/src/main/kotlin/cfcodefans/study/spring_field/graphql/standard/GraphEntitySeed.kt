package cfcodefans.study.spring_field.graphql.standard

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphEntitySeed {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphEntitySeed::class.java)
    }

    @Bean
    open fun loadSampleGraphEntities(repo: GraphEntityRepo, om: ObjectMapper): ApplicationRunner =
        ApplicationRunner { applicationArguments: ApplicationArguments ->
            if (repo.count() > 0) return@ApplicationRunner
            val root: GraphEntity = GraphEntity(entityType = "person",
                                                name = "Ada",
                                                parentId = null,
                                                data = om.readTree("""{"role":"architect"}"""),
                                                note = om.readTree("""{"source":"seed"}"""),
                                                tags = mutableListOf("demo", "root"))
            val savedRoot: GraphEntity = repo.save(root)
            repo.save(GraphEntity(entityType = "document",
                                  name = "Design notes",
                                  parentId = savedRoot.id,
                                  data = om.readTree("""{"pages":3}"""),
                                  note = null,
                                  tags = mutableListOf("nested")))
            repo.save(GraphEntity(entityType = "node",
                                  name = "Orphan node",
                                  parentId = null,
                                  data = null,
                                  note = om.readTree("""{}"""),
                                  tags = mutableListOf()))
            log.info("Seeded ${repo.count()} graph_entity rows (${applicationArguments.sourceArgs.size} startup args)")
        }
}
