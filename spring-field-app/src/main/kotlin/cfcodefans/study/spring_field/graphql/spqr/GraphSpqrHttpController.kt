package cfcodefans.study.spring_field.graphql.spqr

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
@RequestMapping("/graphql")
open class GraphSpqrHttpController(private val graphQL: GraphQL,
                                   private val graphQLSchema: GraphQLSchema,
                                   private val objectMapper: ObjectMapper) {

    @GetMapping("/schema", produces = [MediaType.TEXT_PLAIN_VALUE])
    open fun schema(): ResponseEntity<String> {
        val sdl: String = SchemaPrinter().print(graphQLSchema)
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .contentType(MediaType.TEXT_PLAIN)
            .body(sdl)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun execute(@RequestBody body: JsonNode): ResponseEntity<Map<String, Any?>> {
        val query: String = body.path("query").asText(null) ?: throw IllegalArgumentException("missing \"query\"")
        val operationName: String? = body.path("operationName").asText(null)
        val variablesNode: JsonNode = body.path("variables")
        val variables: Map<String, Any> = if (!variablesNode.isObject) {
            emptyMap()
        } else {
            objectMapper.convertValue(variablesNode, object : TypeReference<Map<String, Any>>() {})
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
