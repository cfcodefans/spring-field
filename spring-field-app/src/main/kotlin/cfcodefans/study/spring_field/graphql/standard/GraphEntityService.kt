package cfcodefans.study.spring_field.graphql.standard

import cfcodefans.study.spring_field.commons.Jsons2
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
open class GraphEntityService(private val repo: IGraphEntityRepo) {
    private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Single list entry point: optional [filter] becomes dynamic `WHERE` clauses (AND).
     * Replaces separate `entitiesByType` / `entitiesByParent`-style operations for study.
     */
    open fun findWithFilter(filter: GraphEntityFilterInput?): List<GraphEntityGql> {
        val spec: Specification<GraphEntity> = filter.toSpecification()
        return repo.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).map { row: GraphEntity -> toGql(row) }
    }

    open fun findById(id: Long): GraphEntityGql? = repo
        .findByIdOrNull(id)
        ?.let { found: GraphEntity -> toGql(found) }

    @Transactional
    open fun create(input: CreateGraphEntityInput): GraphEntityGql = GraphEntity(entityType = input.entityType,
                                                                                 name = input.name,
                                                                                 parentId = input.parentId?.toLongId(),
                                                                                 data = Jsons2.read(input.data),
                                                                                 note = Jsons2.read(input.note),
                                                                                 tags = (input.tags ?: emptyList<String>()).toMutableList())
        .let { repo.save(it) }
        .let { toGql(it) }

    @Transactional
    open fun update(input: UpdateGraphEntityInput): GraphEntityGql {
        val id: Long = input.id.toLongIdRequired()
        val e: GraphEntity = repo.findByIdOrNull(id) ?: throw NoSuchElementException("entity $id not found")
        input.entityType?.let { v: String -> e.entityType = v }
        input.name?.let { v: String -> e.name = v }
        input.parentId?.let { v: String -> e.parentId = v.toLongId() }
        input.data?.let { v: String -> e.data = Jsons2.read(v) }
        input.note?.let { v: String -> e.note = Jsons2.read(v) }
        input.tags?.let { v: List<String> -> e.tags = v.toMutableList() }
        return toGql(repo.save(e))
    }

    @Transactional
    open fun delete(id: Long): Boolean {
        if (!repo.existsById(id)) return false
        repo.deleteById(id)
        return true
    }

    private fun toGql(e: GraphEntity): GraphEntityGql = GraphEntityGql(id = e.id,
                                                                       entityType = e.entityType,
                                                                       name = e.name,
                                                                       parentId = e.parentId,
                                                                       data = Jsons2.toString(e.data),
                                                                       note = Jsons2.toString(e.note),
                                                                       tags = e.tags.toList(),
                                                                       createdAt = isoFmt.format(e.createdAt.atOffset(ZoneOffset.UTC)),
                                                                       updatedAt = isoFmt.format(e.updatedAt.atOffset(ZoneOffset.UTC)))

    private fun String.toLongIdRequired(): Long =
        trim().toLongOrNull() ?: throw IllegalArgumentException("invalid id: $this")

    private fun String?.toLongId(): Long? =
        this?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.toLongOrNull()
}

data class GraphEntityGql(
        val id: Long,
        val entityType: String,
        val name: String,
        val parentId: Long?,
        val data: String?,
        val note: String?,
        val tags: List<String>,
        val createdAt: String,
        val updatedAt: String,
)

data class CreateGraphEntityInput(
        val entityType: String,
        val name: String,
        val parentId: String? = null,
        val data: String? = null,
        val note: String? = null,
        val tags: List<String>? = null,
)

data class UpdateGraphEntityInput(
        val id: String,
        val entityType: String? = null,
        val name: String? = null,
        val parentId: String? = null,
        val data: String? = null,
        val note: String? = null,
        val tags: List<String>? = null,
)

/** Mirrors `GraphEntityFilterInput` in GraphQL schema; bound from GraphQL variables / literals. */
data class GraphEntityFilterInput(
        val entityType: String? = null,
        val parentId: String? = null,
        val rootOnly: Boolean? = null,
        val nameContains: String? = null,
)

private fun GraphEntityFilterInput?.toSpecification(): Specification<GraphEntity> {
    if (this == null) {
        return Specification { _, _, cb -> cb.conjunction() }
    }
    val parts: MutableList<Specification<GraphEntity>> = mutableListOf()
    entityType?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { et: String ->
        parts += Specification { root, _, cb ->
            cb.equal(root.get<Any>("entityType"), et)
        }
    }
    val parentIdLong: Long? = parentId?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.toLongOrNull()
    if (parentIdLong != null) {
        parts += Specification { root, _, cb ->
            cb.equal(root.get<Long>("parentId"), parentIdLong)
        }
    } else if (rootOnly == true) {
        parts += Specification { root, _, cb ->
            cb.isNull(root.get<Long>("parentId"))
        }
    }
    nameContains?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { needle: String ->
        val escaped: String = needle.lowercase()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val pattern: String = "%$escaped%"
        parts += Specification { root, _, cb ->
            val namePath = cb.lower(root.get("name"))
            cb.like(namePath, pattern, '\\')
        }
    }
    if (parts.isEmpty()) {
        return Specification { _, _, cb -> cb.conjunction() }
    }
    return parts.reduce { acc: Specification<GraphEntity>, next: Specification<GraphEntity> -> acc.and(next) }
}
