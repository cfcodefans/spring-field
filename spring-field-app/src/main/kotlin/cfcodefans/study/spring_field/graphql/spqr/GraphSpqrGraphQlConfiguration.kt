package cfcodefans.study.spring_field.graphql.spqr

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import io.leangen.graphql.GraphQLSchemaGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphSpqrGraphQlConfiguration {
    companion object {
        val log: Logger = LoggerFactory.getLogger(GraphSpqrGraphQlConfiguration::class.java)
    }

    @Bean
    open fun graphQLSchema(graphEntityService: GraphEntityService): GraphQLSchema {
        val schema: GraphQLSchema = GraphQLSchemaGenerator()
            .withOperationsFromSingleton(graphEntityService)
            .generate()
        val sdlPreview: String = SchemaPrinter().print(schema)
        log.info("SPQR runtime schema (SDL preview):\n$sdlPreview")
        return schema
    }

    @Bean
    open fun graphQL(schema: GraphQLSchema): GraphQL =
        GraphQL.newGraphQL(schema).build()
}
