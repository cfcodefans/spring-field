package cfcodefans.study.spring_field.api.query.spqr

import cfcodefans.study.spring_field.api.query.GraphEntity
import cfcodefans.study.spring_field.api.query.IGraphEntityRepo
import cfcodefans.study.spring_field.api.query.graphql.CreateGraphEntityInput
import cfcodefans.study.spring_field.api.query.graphql.UpdateGraphEntityInput
import cfcodefans.study.spring_field.commons.Jsons2
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import io.leangen.graphql.GraphQLSchemaGenerator
import io.leangen.graphql.annotations.GraphQLArgument
import io.leangen.graphql.annotations.GraphQLIgnore
import io.leangen.graphql.annotations.GraphQLMutation
import io.leangen.graphql.annotations.GraphQLQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
open class GraphEntitySpqrService(private val repo: IGraphEntityRepo) {
    private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    @GraphQLIgnore
    open fun findAll(): List<GraphEntityGql> =
        repo.findAll().map { row: GraphEntity -> toGql(row) }

    @GraphQLIgnore
    open fun findById(id: Long): GraphEntityGql? =
        repo.findByIdOrNull(id)?.let { found: GraphEntity -> toGql(found) }

    @GraphQLIgnore
    open fun findByEntityType(entityType: String): List<GraphEntityGql> =
        repo.findByEntityType(entityType).map { row: GraphEntity -> toGql(row) }

    @GraphQLIgnore
    open fun findByParent(parentId: Long?): List<GraphEntityGql> {
        val rows: List<GraphEntity> =
            if (parentId == null) repo.findByParentIdIsNull()
            else repo.findByParentId(parentId)
        return rows.map { row: GraphEntity -> toGql(row) }
    }

    @GraphQLIgnore
    @Transactional
    open fun create(input: CreateGraphEntityInput): GraphEntityGql {
        val e: GraphEntity = GraphEntity(
                entityType = input.entityType,
                name = input.name,
                parentId = input.parentId?.toLongId(),
                data = parseJson(input.data),
                note = parseJson(input.note),
                tags = (input.tags ?: emptyList<String>()).toMutableList(),
        )
        return toGql(repo.save(e))
    }

    @GraphQLIgnore
    @Transactional
    open fun update(input: UpdateGraphEntityInput): GraphEntityGql {
        val id: Long = input.id.toLongIdRequired()
        val e: GraphEntity = repo.findByIdOrNull(id) ?: throw NoSuchElementException("entity $id not found")
        input.entityType?.let { v: String -> e.entityType = v }
        input.name?.let { v: String -> e.name = v }
        input.parentId?.let { v: String -> e.parentId = v.toLongId() }
        input.data?.let { v: String -> e.data = parseJson(v) }
        input.note?.let { v: String -> e.note = parseJson(v) }
        input.tags?.let { v: List<String> -> e.tags = v.toMutableList() }
        return toGql(repo.save(e))
    }

    @GraphQLIgnore
    @Transactional
    open fun delete(id: Long): Boolean {
        if (!repo.existsById(id)) return false
        repo.deleteById(id)
        return true
    }

    @GraphQLQuery
    open fun entities(): List<GraphEntityGql> = findAll()

    @GraphQLQuery
    open fun entity(@GraphQLArgument(name = "id") id: String): GraphEntityGql? =
        findById(id.toLongIdRequired())

    @GraphQLQuery
    open fun entitiesByType(@GraphQLArgument(name = "entityType") entityType: String): List<GraphEntityGql> =
        findByEntityType(entityType)

    @GraphQLQuery
    open fun entitiesByParent(@GraphQLArgument(name = "parentId") parentId: String?): List<GraphEntityGql> =
        findByParent(parentId?.toLongId())

//    @GraphQLMutation
//    open fun createEntity(@GraphQLArgument(name = "input") input: CreateGraphEntityInput): GraphEntityGql =
//        create(input)
//
//    @GraphQLMutation
//    open fun updateEntity(@GraphQLArgument(name = "input") input: UpdateGraphEntityInput): GraphEntityGql =
//        update(input)

    @GraphQLMutation
    open fun deleteEntity(@GraphQLArgument(name = "id") id: String): Boolean =
        delete(id.toLongIdRequired())

    private fun parseJson(raw: String?): MutableMap<String, Any?>? = Jsons2.readToMutableMap(raw)

    private fun toGql(e: GraphEntity): GraphEntityGql = GraphEntityGql(
            id = e.id,
            entityType = e.entityType,
            name = e.name,
            parentId = e.parentId,
            data = Jsons2.toString(e.data),
            note = Jsons2.toString(e.note),
            tags = e.tags.toList(),
            createdAt = isoFmt.format(e.createdAt.atOffset(ZoneOffset.UTC)),
            updatedAt = isoFmt.format(e.updatedAt.atOffset(ZoneOffset.UTC)),
    )

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

//data class CreateGraphEntityInput(
//        val entityType: String,
//        val name: String,
//        val parentId: String? = null,
//        val data: String? = null,
//        val note: String? = null,
//        val tags: List<String>? = null,
//)
//
//data class UpdateGraphEntityInput(
//        val id: String,
//        val entityType: String? = null,
//        val name: String? = null,
//        val parentId: String? = null,
//        val data: String? = null,
//        val note: String? = null,
//        val tags: List<String>? = null,
//)

@Configuration
open class GraphSpqrSupportBeans {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphSpqrSupportBeans::class.java)
    }

    @Bean
    open fun graphQLSchema(graphEntityService: GraphEntitySpqrService): GraphQLSchema {
        val schema: GraphQLSchema = GraphQLSchemaGenerator()
            .withOperationsFromSingleton(graphEntityService, GraphEntitySpqrService::class.java)
            .generate()
        val sdlPreview: String = SchemaPrinter().print(schema)
        log.info("SPQR runtime schema (SDL preview):\n$sdlPreview")
        return schema
    }

    @Bean
    open fun graphQL(schema: GraphQLSchema): GraphQL =
        GraphQL.newGraphQL(schema).build()
}