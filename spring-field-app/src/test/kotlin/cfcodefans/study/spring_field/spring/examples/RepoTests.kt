package cfcodefans.study.spring_field.spring.examples.repo

import cfcodefans.study.spring_field.commons.Jsons2
import cfcodefans.study.spring_field.spring.boot.AutoCfgWithoutSecurity
import cfcodefans.study.spring_field.spring.boot.AutoCfgWithoutServletWebStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.PredicateSpecification
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.jvm.optionals.getOrNull

/**
 * File-backed H2 under `<projectRoot>/temp/h2` (git-ignored). No extra `application-*.properties`.
 */
private object RepoTestDataDirs {
    const val TEMP_DIR_NAME: String = "temp"
    const val H2_SUBDIR: String = "h2"
    const val H2_DB_NAME: String = "repotests"

    fun projectRoot(): Path = Paths.get("").toAbsolutePath().normalize()

    fun tempRoot(): Path = projectRoot().resolve(TEMP_DIR_NAME)

    fun h2Directory(): Path = tempRoot().resolve(H2_SUBDIR)

    /** Creates `temp/h2` for H2 file storage (`repotests.mv.db`, etc.). */
    fun ensureH2Directory(): Path = h2Directory().also { Files.createDirectories(it) }
}

private object RepoTestSpringProps {
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

/**
 * Generic graph-style entity: hierarchical nodes with JSON payloads and tags.
 * `entityType` maps the requested "type" classification (node, edge, person, document, …).
 *
 * Business identity is [name] + [lineage] (surrogate [id] is intentionally excluded so equality
 * is stable before and after persistence).
 */
@Entity
@Table(name = "graph_entity",
       indexes = [Index(name = "ix_graph_entity_entity_type", columnList = "entity_type"),
           Index(name = "ix_graph_entity_name", columnList = "name"),
           Index(name = "ix_graph_entity_parent_id", columnList = "parent_id"),
           Index(name = "ix_graph_entity_lineage", columnList = "lineage")])
@EntityListeners(AuditingEntityListener::class)
open class GraphEntity(@Id
                       @GeneratedValue(strategy = GenerationType.IDENTITY)
                       open var id: Long = 0,

                       @Column(name = "entity_type", nullable = false, length = 64)
                       open var entityType: String = "",

                       @Column(nullable = false, length = 512)
                       open var name: String = "",

                       @Column(name = "parent_id")
                       open var parentId: Long? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "lineage")
                       open var lineage: MutableList<String>? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "data")
                       open var data: JsonNode? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "note")
                       open var note: JsonNode? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "tags", nullable = false)
                       open var tags: MutableList<String> = mutableListOf(),

                       @CreatedDate
                       @Column(name = "created_at", nullable = false, updatable = false)
                       open var createdAt: Instant = Instant.now(),

                       @LastModifiedDate
                       @Column(name = "updated_at", nullable = false)
                       open var updatedAt: Instant = Instant.now()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphEntity) return false
        val otherEntity: GraphEntity = other
        return name == otherEntity.name && lineage == otherEntity.lineage
    }

    override fun hashCode(): Int = Objects.hash(name, lineage)

    override fun toString(): String = Jsons2.toString(this)
}

@Repository
interface IGraphEntityRepo : JpaRepository<GraphEntity, Long>, JpaSpecificationExecutor<GraphEntity> {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(IGraphEntityRepo::class.java)
    }

    fun findByEntityType(entityType: String): List<GraphEntity>
    fun findByParentId(parentId: Long): List<GraphEntity>
    fun findByName(name: String): List<GraphEntity>
    fun countByEntityType(entityType: String): Long

    /**
     * Depth-first insert: parent row is saved before any child so [parentId] FKs stay valid.
     * Commits to the file-backed H2 database (not rolled back with test transactions).
     */
    @Transactional
    fun insertTree(root: GraphEntityNode): Int {
        var inserted: Int = 0

        fun persist(node: GraphEntityNode, parentId: Long?) {
            val entity: GraphEntity = node.entity
            entity.parentId = parentId
            val saved: GraphEntity = save(entity)
            log.info("saved ${saved.name}")
            inserted++
            for (child: GraphEntityNode in node.children) {
                persist(child, saved.id)
            }
        }

        persist(root, null)
        return inserted
    }
}

/** In-memory tree node; [entity.parentId] is assigned during [insertTree]. */
data class GraphEntityNode(val entity: GraphEntity,
                           val children: List<GraphEntityNode> = emptyList()) {
    fun size(): Int = 1 + children.sumOf { child: GraphEntityNode -> child.size() }
}


/**
 * Walks the file system under [root] and builds a [GraphEntityNode] tree (no database I/O).
 * [lineage] is the list of ancestor directory names under [root]; [name] is the entry file name.
 */
object FsGraphEntityGenerator {
    private val log: Logger = LoggerFactory.getLogger(FsGraphEntityGenerator::class.java)
    private val json: ObjectMapper = ObjectMapper()

    private val SKIP_DIR_NAMES: Set<String> = setOf(
            ".git", ".idea", ".gradle", ".mvn", ".cursor",
            "target", "build", "node_modules", "out", "dist",
            RepoTestDataDirs.TEMP_DIR_NAME,
    )

    const val MAX_DEPTH: Int = 16
    const val MAX_ENTRIES: Int = 3000

    /** Maven module root (`spring-field-app` when tests run via `mvn test`). */
    fun projectRoot(): Path = RepoTestDataDirs.projectRoot()

    fun defaultRoot(): Path = projectRoot()

    fun buildTree(root: Path = defaultRoot()): GraphEntityNode {
        require(Files.isDirectory(root)) { "Seed root must be a directory: $root" }
        val remaining: IntArray = intArrayOf(MAX_ENTRIES)
        val tree: GraphEntityNode = buildNode(path = root, root = root, depth = 0, remaining = remaining)
        log.info("FsGraphEntityGenerator built tree of ${tree.size()} nodes (${remaining[0]} remaining budget) from $root")
        return tree
    }

    private fun buildNode(path: Path, root: Path, depth: Int, remaining: IntArray): GraphEntityNode {
        val entity: GraphEntity = toGraphEntity(path, root)
        remaining[0]--

        val children: List<GraphEntityNode> =
            if (remaining[0] <= 0 || depth >= MAX_DEPTH || !Files.isDirectory(path)) {
                emptyList()
            } else {
                listChildren(path, root).mapNotNull { childPath: Path ->
                    if (remaining[0] <= 0) null
                    else buildNode(childPath, root, depth + 1, remaining)
                }
            }

        return GraphEntityNode(entity = entity, children = children)
    }

    private fun listChildren(directory: Path, root: Path): List<Path> =
        Files.list(directory).use { stream ->
            stream
                .filter { path: Path -> shouldInclude(path, root) }
                .sorted(compareBy { path: Path -> path.toString() })
                .toList()
        }

    private fun shouldInclude(path: Path, root: Path): Boolean {
        if (!path.startsWith(root)) return false
        if (path.nameCount < root.nameCount) return false
        val rel: Path? = if (path == root) null else root.relativize(path)
        val underSkippedDir: Boolean? = rel?.any { segment: Path ->
            SKIP_DIR_NAMES.contains(segment.name)
        }
        if (underSkippedDir == true) return false
        return try {
            val isDirectory: Boolean = Files.isDirectory(path)
            val isRegularFile: Boolean = Files.isRegularFile(path)
            isDirectory || isRegularFile
        } catch (_: IOException) {
            false
        }
    }

    private fun lineageSegments(path: Path, root: Path): MutableList<String>? {
        if (path == root) return null
        val rel: Path = root.relativize(path)
        if (rel.nameCount <= 1) return null
        return (0 until rel.nameCount - 1)
            .map { index: Int -> rel.getName(index).toString() }
            .toMutableList()
    }

    private fun toGraphEntity(path: Path, root: Path): GraphEntity {
        val directory: Boolean = Files.isDirectory(path)
        val entityType: String = if (directory) "directory" else "file"
        val tags: MutableList<String> = mutableListOf("fs-seed")
        if (!directory) {
            val extension: String? = path.extension.takeIf { ext: String -> ext.isNotEmpty() }
            extension?.let { ext: String -> tags.add("ext:$ext") }
        }

        val data: ObjectNode = json.createObjectNode()
            .put("path", root.relativize(path).toString().replace('\\', '/'))
        if (directory) {
            data.put("entryKind", "directory")
        } else {
            val sizeBytes: Long? = runCatching { Files.size(path) }.getOrNull()
            sizeBytes?.let { bytes: Long -> data.put("sizeBytes", bytes) }
            data.put("entryKind", "file")
        }

        return GraphEntity(
                entityType = entityType,
                name = path.fileName.toString(),
                parentId = null,
                lineage = lineageSegments(path, root),
                data = data,
                tags = tags,
        )
    }
}

@ComponentScan("cfcodefans.study.spring_field.spring.examples.repo")
@EntityScan("cfcodefans.study.spring_field.spring.examples.repo")
@EnableJpaAuditing
@AutoCfgWithoutSecurity
@AutoCfgWithoutServletWebStack
@SpringBootConfiguration
@SpringBootApplication
open class RepoApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RepoApp::class.java)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(RepoApp::class.java, *args)
}

/**
 * JPA repository slice for [GraphEntity] (aligned with `graphql.standard` / `graphql.spqr` models).
 *
 * @see <a href="https://spring.io/guides/gs/accessing-data-jpa/">Accessing Data with JPA</a>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [RepoApp::class],
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                useMainMethod = SpringBootTest.UseMainMethod.WHEN_AVAILABLE,
                properties = [RepoTestSpringProps.ACTIVE_PROFILE_LAB,
                    RepoTestSpringProps.DATASOURCE_URL,
                    RepoTestSpringProps.DATASOURCE_DRIVER,
                    RepoTestSpringProps.DATASOURCE_USERNAME,
                    RepoTestSpringProps.DATASOURCE_PASSWORD,
                    RepoTestSpringProps.JPA_DDL_AUTO,
                    RepoTestSpringProps.JPA_SHOW_SQL,
                    RepoTestSpringProps.JPA_OPEN_IN_VIEW,
                    RepoTestSpringProps.JPA_TIME_ZONE])
open class RepoTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(RepoTests::class.java)
    }

    @Autowired
    open lateinit var graphRepo: IGraphEntityRepo

    @BeforeEach
    open fun prepareDatabase() {
        val h2Dir: Path = RepoTestDataDirs.ensureH2Directory()
        val rowCount: Long = graphRepo.count()
        log.info("H2 database directory: $h2Dir (files: repotests.mv.db, …); rows=$rowCount")
    }

    /**
     * No [@Transactional] here — Spring Test would roll back and nothing would reach `temp/h2`.
     * Seed runs only when the database is empty so reruns keep data without duplicating the tree.
     */
    @Test
    open fun seedFromFileSystemAndQuery() {
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

        val directories: List<GraphEntity> = graphRepo.findByEntityType("directory")
        val files: List<GraphEntity> = graphRepo.findByEntityType("file")
        assertFalse(directories.isEmpty(), "expected directory rows in persisted database")
        assertTrue(files.isNotEmpty(), "expected file rows in persisted database")

        val rootName: String = root.fileName.toString()
        val rootDirs: List<GraphEntity> = directories.filter { entity: GraphEntity ->
            entity.parentId == null && entity.name == rootName
        }
        assertTrue(rootDirs.isNotEmpty(), "expected root directory node named $rootName")
        val rootDir: GraphEntity = rootDirs.first()
        val children: List<GraphEntity> = graphRepo.findByParentId(rootDir.id)
        assertTrue(children.isNotEmpty())

        val repoTestFiles: List<GraphEntity> = graphRepo.findByName("RepoTests.kt")
        assertTrue(repoTestFiles.isNotEmpty(), "RepoTests.kt should appear in the file tree")
        val resolvedTestFile: GraphEntity = repoTestFiles.first()
        val lineage: MutableList<String> = resolvedTestFile.lineage!!
        assertTrue(lineage.contains("examples"))

        val totalCount: Long = graphRepo.count()
        val fileCount: Long = graphRepo.countByEntityType("file")
        assertTrue(totalCount > 0)
        assertTrue(fileCount >= 1)
        if (seeded > 0) {
            assertEquals(seeded.toLong(), totalCount)
        } else {
            assertTrue(totalCount >= countBefore)
        }
        log.info(
                "database has $totalCount rows (${directories.size} dirs, ${files.size} files); seeded $seeded this run",
        )
    }

    @Test
    open fun testSpecifications() {
        run {
            val ge: GraphEntity? = PredicateSpecification
                .unrestricted<GraphEntity>()
                .and { from, cb -> from.get<String>("name").equalTo("RepoTests.kt") }
                .let { graphRepo.findOne(it) }
                .getOrNull()
            log.info(ge.toString())
        }
    }
}
