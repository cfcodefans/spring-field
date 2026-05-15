package cfcodefans.study.spring_field.graphql.standard

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
open class GraphqlCtrl(private val service: GraphEntityService) {
    @QueryMapping
    open fun entities(): List<GraphEntityGql> = service.findAll()

    @QueryMapping
    open fun entity(@Argument id: String): GraphEntityGql? =
        service.findById(id.requireLong("id"))

    @QueryMapping
    open fun entitiesByType(@Argument entityType: String): List<GraphEntityGql> =
        service.findByEntityType(entityType)

    @QueryMapping
    open fun entitiesByParent(@Argument(name = "parentId") parentId: String?): List<GraphEntityGql> =
        service.findByParent(parentId.parseLongOrNull())

    @MutationMapping
    open fun createEntity(@Argument input: CreateGraphEntityInput): GraphEntityGql =
        service.create(input)

    @MutationMapping
    open fun updateEntity(@Argument input: UpdateGraphEntityInput): GraphEntityGql =
        service.update(input)

    @MutationMapping
    open fun deleteEntity(@Argument id: String): Boolean =
        service.delete(id.requireLong("id"))

    private fun String.requireLong(field: String): Long =
        trim().toLongOrNull() ?: throw IllegalArgumentException("invalid $field: $this")

    private fun String?.parseLongOrNull(): Long? =
        this?.trim()?.takeIf { text: String -> text.isNotEmpty() }?.toLongOrNull()
}
