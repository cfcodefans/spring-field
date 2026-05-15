package cfcodefans.study.spring_field.graphql.standard

import cfcodefans.study.spring_field.commons.Jsons2
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
    open fun loadSampleGraphEntities(repo: GraphEntityRepo): ApplicationRunner =
        ApplicationRunner { applicationArguments: ApplicationArguments ->
            if (repo.count() > 0) return@ApplicationRunner
            val root: GraphEntity = GraphEntity(entityType = "person",
                                                name = "Ada",
                                                parentId = null,
                                                data = Jsons2.read("""{"role":"architect"}"""),
                                                note = Jsons2.read("""{"source":"seed"}"""),
                                                tags = mutableListOf("demo", "root"))
            val savedRoot: GraphEntity = repo.save(root)
            repo.save(GraphEntity(entityType = "document",
                                  name = "Design notes",
                                  parentId = savedRoot.id,
                                  data = Jsons2.read("""{"pages":3}"""),
                                  note = null,
                                  tags = mutableListOf("nested")))
            repo.save(GraphEntity(entityType = "node",
                                  name = "Orphan node",
                                  parentId = null,
                                  data = null,
                                  note = Jsons2.read("""{}"""),
                                  tags = mutableListOf()))
            log.info("Seeded ${repo.count()} graph_entity rows (${applicationArguments.sourceArgs.size} startup args)")
        }
}
