package cfcodefans.study.spring_field.graphql.standard

import cfcodefans.study.spring_field.commons.Jsons2
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
open class GraphEntityService(private val repo: GraphEntityRepo) {
    private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    open fun findAll(): List<GraphEntityGql> = repo
        .findAll()
        .map { row: GraphEntity -> toGql(row) }

    open fun findById(id: Long): GraphEntityGql? = repo
        .findByIdOrNull(id)
        ?.let { found: GraphEntity -> toGql(found) }

    open fun findByEntityType(entityType: String): List<GraphEntityGql> = repo
        .findByEntityType(entityType)
        .map { row: GraphEntity -> toGql(row) }

    open fun findByParent(parentId: Long?): List<GraphEntityGql> = (
            if (parentId == null)
                repo.findByParentIdIsNull()
            else
                repo.findByParentId(parentId)
            ).map { row: GraphEntity -> toGql(row) }

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
