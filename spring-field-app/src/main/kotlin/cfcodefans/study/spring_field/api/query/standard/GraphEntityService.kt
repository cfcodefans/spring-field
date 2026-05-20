package cfcodefans.study.spring_field.api.query.standard

import cfcodefans.study.spring_field.api.query.GraphEntity
import cfcodefans.study.spring_field.api.query.IGraphEntityRepo
import cfcodefans.study.spring_field.commons.Jsons2
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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

/** Mirrors `InstantRangeInput` in GraphQL schema. */
data class InstantRangeInput(
        val from: String,
        val to: String,
)

/** Mirrors `GraphEntityFilterInput` in GraphQL schema; bound from GraphQL variables / literals. */
data class GraphEntityFilterInput(
        val id: String? = null,
        val entityType: String? = null,
        val entityTypeIn: List<String>? = null,
        val parentId: String? = null,
        val rootOnly: Boolean? = null,
        val nameLike: String? = null,
        val nameContains: String? = null,
        val dataLike: String? = null,
        val noteLike: String? = null,
        val updatedAtBetween: InstantRangeInput? = null,
)

private const val MAX_ENTITY_TYPE_IN: Int = 100

private fun GraphEntityFilterInput?.toSpecification(): Specification<GraphEntity> {
    if (this == null) {
        return unrestrictedSpec()
    }
    val parts: MutableList<Specification<GraphEntity>> = mutableListOf()
    id?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.toLongOrNull()?.let { idLong: Long ->
        parts += equalLongSpec("id", idLong)
    }
    entityType?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { et: String ->
        parts += equalStringSpec("entityType", et)
    }
    entityTypeIn?.map { value: String -> value.trim() }
        ?.filter { text: String -> text.isNotEmpty() }
        ?.distinct()
        ?.takeIf { values: List<String> -> values.isNotEmpty() }
        ?.let { types: List<String> ->
            require(types.size <= MAX_ENTITY_TYPE_IN) {
                "entityTypeIn supports at most $MAX_ENTITY_TYPE_IN values"
            }
            parts += Specification { root, _, cb ->
                root.get<String>("entityType").`in`(types)
            }
        }
    val parentIdLong: Long? = parentId?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.toLongOrNull()
    if (parentIdLong != null) {
        parts += equalLongSpec("parentId", parentIdLong)
    } else if (rootOnly == true) {
        parts += Specification { root, _, cb ->
            cb.isNull(root.get<Long>("parentId"))
        }
    }
    nameLike?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { pattern: String ->
        parts += stringLikeSpec("name", pattern, wrapContains = false)
    }
    nameContains?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { needle: String ->
        parts += stringLikeSpec("name", needle, wrapContains = true)
    }
    dataLike?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { pattern: String ->
        parts += jsonLikeSpec("data", pattern)
    }
    noteLike?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.let { pattern: String ->
        parts += jsonLikeSpec("note", pattern)
    }
    updatedAtBetween?.let { range: InstantRangeInput ->
        val rangeStart: Instant = Instant.parse(range.from.trim())
        val rangeEnd: Instant = Instant.parse(range.to.trim())
        parts += Specification { root, _, cb ->
            val updatedAtPath = root.get<Instant>("updatedAt")
            cb.between(updatedAtPath, rangeStart, rangeEnd)
        }
    }
    if (parts.isEmpty()) {
        return unrestrictedSpec()
    }
    return parts.reduce { acc: Specification<GraphEntity>, next: Specification<GraphEntity> -> acc.and(next) }
}

private fun unrestrictedSpec(): Specification<GraphEntity> =
    Specification { _, _, cb -> cb.conjunction() }

private fun equalStringSpec(attribute: String, value: String): Specification<GraphEntity> =
    Specification { root, _, cb -> cb.equal(root.get<String>(attribute), value) }

private fun equalLongSpec(attribute: String, value: Long): Specification<GraphEntity> =
    Specification { root, _, cb -> cb.equal(root.get<Long>(attribute), value) }

private fun stringLikeSpec(attribute: String, rawPattern: String, wrapContains: Boolean): Specification<GraphEntity> {
    val pattern: String = if (wrapContains) {
        "%${escapeLikeLiteral(rawPattern)}%"
    } else {
        rawPattern.lowercase()
    }
    return Specification { root, _, cb ->
        val path = cb.lower(root.get<String>(attribute))
        cb.like(path, pattern, '\\')
    }
}

private fun jsonLikeSpec(attribute: String, rawPattern: String): Specification<GraphEntity> {
    val pattern: String = rawPattern.lowercase()
    return Specification { root, _, cb ->
        val jsonAsString = root.get<Any>(attribute).`as`(String::class.java)
        val path = cb.lower(jsonAsString)
        cb.like(path, pattern, '\\')
    }
}

private fun escapeLikeLiteral(needle: String): String = needle.lowercase()
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")
