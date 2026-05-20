package cfcodefans.study.spring_field.api.query.odata

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.apache.olingo.commons.api.edm.provider.*
import org.springframework.stereotype.Component

/**
 * Minimal OData CSDL for [cfcodefans.study.spring_field.graphql.GraphEntity].
 * Property names match JPA attribute names used in [ODataFilterSpecificationVisitor].
 */
@Component
open class GraphEntityODataEdmProvider : CsdlAbstractEdmProvider() {
    override fun getSchemas(): MutableList<CsdlSchema> {
        val entityType: CsdlEntityType = CsdlEntityType()
            .setName(ENTITY_TYPE_NAME)
            .setProperties(
                listOf(
                    primitiveProperty("id", EdmPrimitiveTypeKind.Int64, false),
                    primitiveProperty("entityType", EdmPrimitiveTypeKind.String, false),
                    primitiveProperty("name", EdmPrimitiveTypeKind.String, false),
                    primitiveProperty("parentId", EdmPrimitiveTypeKind.Int64, true),
                    primitiveProperty("data", EdmPrimitiveTypeKind.String, true),
                    primitiveProperty("note", EdmPrimitiveTypeKind.String, true),
                    primitiveProperty("createdAt", EdmPrimitiveTypeKind.DateTimeOffset, false),
                    primitiveProperty("updatedAt", EdmPrimitiveTypeKind.DateTimeOffset, false),
                ),
            )
            .setKey(listOf(CsdlPropertyRef().setName("id")))
        val entitySet: CsdlEntitySet = CsdlEntitySet()
            .setName(ENTITY_SET_NAME)
            .setType(ENTITY_TYPE_FQN)
        val container: CsdlEntityContainer = CsdlEntityContainer()
            .setName(CONTAINER_NAME)
            .setEntitySets(listOf(entitySet))
        val schema: CsdlSchema = CsdlSchema()
            .setNamespace(NAMESPACE)
            .setEntityTypes(listOf(entityType))
            .setEntityContainer(container)
        return mutableListOf(schema)
    }

    override fun getEntityContainer(): CsdlEntityContainer? = getSchemas().firstOrNull()?.entityContainer

    override fun getEntityContainerInfo(FQN: FullQualifiedName?): CsdlEntityContainerInfo? =
        CsdlEntityContainerInfo().setContainerName(CONTAINER_FQN)

    override fun getEntityType(entityTypeName: FullQualifiedName?): CsdlEntityType? =
        if (ENTITY_TYPE_FQN == entityTypeName) getSchemas().first().entityTypes.firstOrNull() else null

    override fun getEntitySet(container: FullQualifiedName?, entitySetName: String?): CsdlEntitySet? =
        if (CONTAINER_FQN == container && ENTITY_SET_NAME == entitySetName) {
            CsdlEntitySet().setName(ENTITY_SET_NAME).setType(ENTITY_TYPE_FQN)
        } else {
            null
        }

    companion object {
        const val NAMESPACE: String = "GraphStandard"
        const val CONTAINER_NAME: String = "Container"
        const val ENTITY_TYPE_NAME: String = "GraphEntity"
        const val ENTITY_SET_NAME: String = "GraphEntities"
        val ENTITY_TYPE_FQN: FullQualifiedName = FullQualifiedName(NAMESPACE, ENTITY_TYPE_NAME)
        val CONTAINER_FQN: FullQualifiedName = FullQualifiedName(NAMESPACE, CONTAINER_NAME)

        private fun primitiveProperty(name: String, kind: EdmPrimitiveTypeKind, nullable: Boolean): CsdlProperty =
            CsdlProperty()
                .setName(name)
                .setType(kind.fullQualifiedName)
                .setNullable(nullable)
    }
}
