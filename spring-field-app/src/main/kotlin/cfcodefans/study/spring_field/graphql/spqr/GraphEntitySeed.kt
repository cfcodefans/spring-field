package cfcodefans.study.spring_field.graphql.spqr

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import cfcodefans.study.spring_field.commons.Jsons2
import com.fasterxml.jackson.databind.node.ObjectNode
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
        ApplicationRunner { appArgs: ApplicationArguments ->
            if (repo.count() > 0) return@ApplicationRunner
            val root: GraphEntity = GraphEntity(
                    entityType = "person",
                    name = "Ada",
                    parentId = null,
                    data = Jsons2.read("""{"role":"architect"}""", ObjectNode::class.java),
                    note = Jsons2.read("""{"source":"seed"}""", ObjectNode::class.java),
                    tags = mutableListOf("demo", "root"),
            )
            val savedRoot: GraphEntity = repo.save(root)
            repo.save(
                    GraphEntity(
                            entityType = "document",
                            name = "Design notes",
                            parentId = savedRoot.id,
                            data = Jsons2.read("""{"pages":3}""", ObjectNode::class.java),
                            note = null,
                            tags = mutableListOf("nested"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "node",
                            name = "Orphan node",
                            parentId = null,
                            data = null,
                            note = Jsons2.read("""{}""", ObjectNode::class.java),
                            tags = mutableListOf(),
                    ),
            )
            log.info("Seeded ${repo.count()} spqr_graph_entity rows (${appArgs.sourceArgs.size} startup args)")
        }
}
