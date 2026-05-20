package cfcodefans.study.spring_field.api.query

import cfcodefans.study.spring_field.RepoTestDataDirs
import cfcodefans.study.spring_field.commons.Jsons2
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.name

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

/** In-memory tree node; [entity.parentId] is assigned during [insertTree]. */
data class GraphEntityNode(val entity: cfcodefans.study.spring_field.api.query.GraphEntity,
                           val children: List<GraphEntityNode> = emptyList()) {
    fun size(): Int = 1 + children.sumOf { child: GraphEntityNode -> child.size() }
}

/**
 * Walks the file system under [root] and builds a [cfcodefans.study.spring_field.api.query.GraphEntityNode] tree (no database I/O).
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