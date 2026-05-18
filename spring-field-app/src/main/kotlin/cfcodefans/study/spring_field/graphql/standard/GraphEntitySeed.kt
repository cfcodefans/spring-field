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
    open fun loadSampleGraphEntities(repo: IGraphEntityRepo): ApplicationRunner =
        ApplicationRunner { applicationArguments: ApplicationArguments ->
            if (repo.count() > 0) return@ApplicationRunner

            val ada: GraphEntity = repo.save(
                    GraphEntity(
                            entityType = "person",
                            name = "Ada",
                            parentId = null,
                            data = Jsons2.read("""{"role":"architect"}"""),
                            note = Jsons2.read("""{"source":"seed"}"""),
                            tags = mutableListOf("demo", "root", "pioneer"),
                    ),
            )
            val designNotes: GraphEntity = repo.save(
                    GraphEntity(
                            entityType = "document",
                            name = "Design notes",
                            parentId = ada.id,
                            data = Jsons2.read("""{"pages":3,"format":"md"}"""),
                            note = null,
                            tags = mutableListOf("nested", "draft"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "node",
                            name = "Orphan node",
                            parentId = null,
                            data = null,
                            note = Jsons2.read("""{}"""),
                            tags = mutableListOf(),
                    ),
            )

            val grace: GraphEntity = repo.save(
                    GraphEntity(
                            entityType = "person",
                            name = "Grace Hopper",
                            parentId = null,
                            data = Jsons2.read("""{"role":"compiler"}"""),
                            note = Jsons2.read("""{"source":"seed"}"""),
                            tags = mutableListOf("demo", "root", "navy"),
                    ),
            )
            val linus: GraphEntity = repo.save(
                    GraphEntity(
                            entityType = "person",
                            name = "Linus",
                            parentId = ada.id,
                            data = Jsons2.read("""{"role":"maintainer"}"""),
                            note = null,
                            tags = mutableListOf("kernel", "child"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "document",
                            name = "GraphQL cheatsheet",
                            parentId = ada.id,
                            data = Jsons2.read("""{"pages":1,"format":"pdf"}"""),
                            note = Jsons2.read("""{"pinned":true}"""),
                            tags = mutableListOf("reference", "nested"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "task",
                            name = "Review schema indexes",
                            parentId = designNotes.id,
                            data = Jsons2.read("""{"priority":"high","estimateH":2}"""),
                            note = null,
                            tags = mutableListOf("todo", "nested"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "document",
                            name = "Field ops runbook",
                            parentId = grace.id,
                            data = Jsons2.read("""{"pages":42}"""),
                            note = Jsons2.read("""{"owner":"sre"}"""),
                            tags = mutableListOf("ops", "nested"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "node",
                            name = "Cache prime slot",
                            parentId = null,
                            data = Jsons2.read("""{"ttlSec":60}"""),
                            note = null,
                            tags = mutableListOf("infra", "root"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "edge",
                            name = "Ada — cites — Design notes",
                            parentId = ada.id,
                            data = Jsons2.read("""{"fromId":${ada.id},"toId":${designNotes.id},"kind":"cites"}"""),
                            note = null,
                            tags = mutableListOf("graph", "nested"),
                    ),
            )
            repo.save(
                    GraphEntity(
                            entityType = "document",
                            name = "Meeting notes — backlog grooming",
                            parentId = linus.id,
                            data = Jsons2.read("""{"pages":2}"""),
                            note = null,
                            tags = mutableListOf("meeting", "nested"),
                    ),
            )

            log.info("Seeded ${repo.count()} graph_entity rows (${applicationArguments.sourceArgs.size} startup args)")
        }
}
