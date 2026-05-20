package cfcodefans.study.spring_field.api.query

import cfcodefans.study.spring_field.RepoTestDataDirs
import org.junit.jupiter.api.Assertions.assertTrue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import org.springframework.boot.ApplicationArguments as AppArgs
import org.springframework.boot.ApplicationRunner as AppRunner

@Configuration
open class GraphEntitySeed {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphEntitySeed::class.java)
    }

    @Bean
    open fun loadSampleGraphEntities(graphRepo: IGraphEntityRepo): AppRunner = AppRunner { appArgs: AppArgs ->
        val h2Dir: Path = RepoTestDataDirs.ensureH2Directory()
        val rowCount: Long = graphRepo.count()
        log.info("H2 database directory: $h2Dir (files: repotests.mv.db, …); rows=$rowCount")
        if (rowCount > 0) return@AppRunner

        val root: Path = FsGraphEntityGenerator.defaultRoot()
        val countBefore: Long = graphRepo.count()
        val seeded: Int = if (countBefore == 0L) {
            val tree: GraphEntityNode = FsGraphEntityGenerator.buildTree(root)
            assertTrue(tree.size() > 0, "expected at least the root directory under $root")
            graphRepo.insertTree(tree).also { inserted: Int ->
                log.info("inserted $inserted rows into empty database from $root")
            }
        } else {
            log.info("skipping seed; database already has $countBefore rows (delete temp/h2 to reset)")
            0
        }

        log.info("Seeded ${graphRepo.count()} graph_entity rows (${appArgs.sourceArgs.size} startup args)")
    }
}
