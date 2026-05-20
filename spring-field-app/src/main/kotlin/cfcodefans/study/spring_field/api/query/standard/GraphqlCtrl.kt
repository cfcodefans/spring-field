package cfcodefans.study.spring_field.api.query.standard

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/graphql/standard")
open class GraphqlCtrl(private val service: GraphEntityService) {
    @QueryMapping
    open fun entities(@Argument(name = "filter") filter: GraphEntityFilterInput?): List<GraphEntityGql> =
        service.findWithFilter(filter)

    @QueryMapping
    open fun entity(@Argument id: String): GraphEntityGql? =
        service.findById(id.requireLong("id"))

//    @MutationMapping
//    open fun createEntity(@Argument input: CreateGraphEntityInput): GraphEntityGql =
//        service.create(input)
//
//    @MutationMapping
//    open fun updateEntity(@Argument input: UpdateGraphEntityInput): GraphEntityGql =
//        service.update(input)

    @MutationMapping
    open fun deleteEntity(@Argument id: String): Boolean =
        service.delete(id.requireLong("id"))

    private fun String.requireLong(field: String): Long =
        trim().toLongOrNull() ?: throw IllegalArgumentException("invalid $field: $this")
}