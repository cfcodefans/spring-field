package cfcodefans.study.spring_field.api.query.spqr

import cfcodefans.study.spring_field.commons.Jsons2
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Minimal GraphQL-over-HTTP endpoint for the SPQR engine (Spring for GraphQL is not used in this profile).
 */
@RestController
@RequestMapping("/graphql/spqr")
open class GraphSpqrHttpCtrl(private val graphQL: GraphQL,
                             private val graphQLSchema: GraphQLSchema) {

    @GetMapping("/schema", produces = [MediaType.TEXT_PLAIN_VALUE])
    open fun schema(): ResponseEntity<String> {
        val sdl: String = SchemaPrinter().print(graphQLSchema)
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .contentType(MediaType.TEXT_PLAIN)
            .body(sdl)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun execute(@RequestBody body: String): ResponseEntity<Map<String, Any?>> {
        val root: ObjectNode = Jsons2.read(body, ObjectNode::class.java)
        val query: String = root.path("query").asText(null) ?: throw IllegalArgumentException("missing \"query\"")
        val operationName: String? = root.path("operationName").asText(null)?.takeIf { it.isNotEmpty() }
        val variablesNode: JsonNode = root.path("variables")
        val variables: Map<String, Any> = if (!variablesNode.isObject) {
            emptyMap()
        } else {
            Jsons2.MAPPER.convertValue(variablesNode, object : TypeReference<Map<String, Any>>() {})
        }

        val executionInput: ExecutionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .variables(variables)
            .operationName(operationName)
            .build()

        val result: ExecutionResult = graphQL.execute(executionInput)

        @Suppress("UNCHECKED_CAST")
        val spec: Map<String, Any?> = result.toSpecification() as Map<String, Any?>
        return ResponseEntity.ok(spec)
    }
}
