package cfcodefans.study.spring_field.api.query.odata

import cfcodefans.study.spring_field.api.query.GraphEntity
import cfcodefans.study.spring_field.api.query.IGraphEntityRepo
import cfcodefans.study.spring_field.commons.Jsons2
import org.apache.olingo.commons.api.edm.EdmEnumType
import org.apache.olingo.commons.api.edm.EdmType
import org.apache.olingo.commons.api.format.ContentType
import org.apache.olingo.server.api.OData
import org.apache.olingo.server.api.ServiceMetadata
import org.apache.olingo.server.api.uri.UriInfo
import org.apache.olingo.server.api.uri.UriResource
import org.apache.olingo.server.api.uri.UriResourceEntitySet
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty
import org.apache.olingo.server.api.uri.queryoption.OrderByItem
import org.apache.olingo.server.api.uri.queryoption.SelectItem
import org.apache.olingo.server.api.uri.queryoption.SelectOption
import org.apache.olingo.server.api.uri.queryoption.expression.*
import org.apache.olingo.server.core.uri.parser.Parser
import org.apache.olingo.server.core.uri.parser.UriParserException
import org.apache.olingo.server.core.uri.validator.UriValidationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
open class ODataGraphEntityService(private val repo: IGraphEntityRepo,
                                   edmProvider: GraphEntityODataEdmProvider) {
    private val odata: OData = OData.newInstance()
    private val serviceMetadata: ServiceMetadata = odata.createServiceMetadata(edmProvider, emptyList())
    private val parser: Parser = Parser(serviceMetadata.edm, odata)
    private val isoFmt: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    open fun metadataXml(): String {
        val content: InputStream = odata.createSerializer(ContentType.APPLICATION_XML)
            .metadataDocument(serviceMetadata)
            .content
        return content.readAllBytes().toString(StandardCharsets.UTF_8)
    }

    open fun queryEntitySet(resourcePath: String, rawQuery: String?): ODataEntitySetResult {
        val uriInfo: UriInfo = parseUri(resourcePath, rawQuery)
        val spec: Specification<GraphEntity> = ODataFilterSpecificationVisitor.toSpecification(uriInfo.filterOption?.expression)
        val sort: Sort = toSort(uriInfo)
        val topSkip: TopSkip = toTopSkip(uriInfo)
        val selectFields: Set<String>? = toSelectFields(uriInfo)
        val includeCount: Boolean = uriInfo.countOption != null
        val rows: List<GraphEntity> = repo.findAll(spec, sort).drop(topSkip.skip).take(topSkip.top)
        val total: Long? = if (includeCount) repo.count(spec) else null
        return ODataEntitySetResult(context = odataContext(resourcePath, selectFields),
                                    count = total,
                                    value = rows.map { entity: GraphEntity -> toODataMap(entity, selectFields) })
    }

    open fun queryEntityByKey(resourcePath: String, rawQuery: String?): ODataEntityResult {
        val uriInfo: UriInfo = parseUri(resourcePath, rawQuery)
        val id: Long = extractKey(uriInfo)
        val entity: GraphEntity = repo.findByIdOrNull(id)
            ?: throw NoSuchElementException("GraphEntity $id not found")
        val selectFields: Set<String>? = toSelectFields(uriInfo)
        return ODataEntityResult(
                context = odataContext(resourcePath, selectFields),
                entity = toODataMap(entity, selectFields),
        )
    }

    private fun parseUri(resourcePath: String, rawQuery: String?): UriInfo {
        val query: String = rawQuery?.trim()?.removePrefix("?").orEmpty()
        return try {
            parser.parseUri(resourcePath, query, null, null)
        } catch (ex: UriParserException) {
            throw IllegalArgumentException("invalid OData URI: $resourcePath ? $query — ${ex.message}", ex)
        } catch (ex: UriValidationException) {
            throw IllegalArgumentException("invalid OData URI: $resourcePath ? $query — ${ex.message}", ex)
        }
    }

    private fun extractKey(uriInfo: UriInfo): Long = uriInfo
        .uriResourceParts
        .firstOrNull() { part -> part is UriResourceEntitySet && part.keyPredicates.isNotEmpty() }
        ?.let { part -> part as UriResourceEntitySet }
        ?.keyPredicates
        ?.first()
        ?.text
        ?.let { text -> text.toLongOrNull() ?: throw IllegalArgumentException("invalid entity key: $text") }
        ?: throw IllegalArgumentException("missing entity key in path")

    private fun toSort(uriInfo: UriInfo): Sort {
        val orderBy = uriInfo.orderByOption ?: return Sort.by(Sort.Direction.ASC, "id")
        val orders: List<Sort.Order> = orderBy.orders.map { item: OrderByItem ->
            val property: String = item.expression.accept(object : ExpressionVisitor<String> {
                override fun visitMember(member: Member): String {
                    val last: UriResource? = member.resourcePath.uriResourceParts.last()
                    return when (last) {
                        is UriResourcePrimitiveProperty -> last.property.name
                        else -> throw IllegalArgumentException("unsupported \$orderby member")
                    }
                }

                override fun visitLiteral(literal: Literal): String =
                    throw IllegalArgumentException("literal in \$orderby is not supported")

                override fun visitBinaryOperator(operator: BinaryOperatorKind,
                                                 left: String,
                                                 right: String): String = throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitBinaryOperator(operator: BinaryOperatorKind,
                                                 left: String,
                                                 right: MutableList<String>): String = throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitUnaryOperator(operator: UnaryOperatorKind,
                                                operand: String): String = throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitMethodCall(methodCall: MethodKind,
                                             parameters: MutableList<String>): String = throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitLambdaExpression(lambdaFunction: String,
                                                   lambdaVariable: String,
                                                   expression: Expression): String = throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitAlias(aliasName: String): String =
                    throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitTypeLiteral(type: EdmType): String =
                    throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitLambdaReference(variableName: String): String =
                    throw IllegalArgumentException("expression in \$orderby is not supported")

                override fun visitEnum(type: EdmEnumType,
                                       enumValues: MutableList<String>): String =
                    throw IllegalArgumentException("expression in \$orderby is not supported")
            })
            if (item.isDescending) Sort.Order.desc(property) else Sort.Order.asc(property)
        }
        return Sort.by(orders)
    }

    private fun toTopSkip(uriInfo: UriInfo): TopSkip {
        val top: Int = (uriInfo.topOption?.value ?: DEFAULT_TOP).coerceIn(1, MAX_TOP)
        val skip: Int = (uriInfo.skipOption?.value ?: 0).coerceAtLeast(0)
        return TopSkip(top = top, skip = skip)
    }

    private fun toSelectFields(uriInfo: UriInfo): Set<String>? {
        val select: SelectOption = uriInfo.selectOption ?: return null
        val names: Set<String> = select
            .selectItems
            .mapNotNull { item: SelectItem ->
                if (item.isStar) return@mapNotNull null
                val last: UriResource = item.resourcePath.uriResourceParts.lastOrNull() ?: return@mapNotNull null
                when (last) {
                    is UriResourcePrimitiveProperty -> last.property.name
                    else -> null
                }
            }.toSet()
        return names.takeIf { it.isNotEmpty() }
    }

    private fun toODataMap(entity: GraphEntity, selectFields: Set<String>?): Map<String, Any?> {
        val full: Map<String, Any?> = linkedMapOf("id" to entity.id,
                                                  "entityType" to entity.entityType,
                                                  "name" to entity.name,
                                                  "parentId" to entity.parentId,
                                                  "data" to Jsons2.toString(entity.data),
                                                  "note" to Jsons2.toString(entity.note),
                                                  "createdAt" to isoFmt.format(entity.createdAt.atOffset(ZoneOffset.UTC)),
                                                  "updatedAt" to isoFmt.format(entity.updatedAt.atOffset(ZoneOffset.UTC)))
        if (selectFields == null) {
            return full
        }
        return full.filterKeys { key: String -> key in selectFields }
    }

    private fun odataContext(resourcePath: String, selectFields: Set<String>?): String {
        val fragment: String = if (selectFields.isNullOrEmpty()) {
            ""
        } else {
            "(\$select=${selectFields.joinToString(",")})"
        }
        return "\$metadata#${resourcePath}$fragment"
    }

    companion object {
        const val MAX_TOP: Int = 500
        const val DEFAULT_TOP: Int = 500
    }
}

data class ODataEntitySetResult(
        val context: String,
        val count: Long?,
        val value: List<Map<String, Any?>>,
)

data class ODataEntityResult(
        val context: String,
        val entity: Map<String, Any?>,
)

private data class TopSkip(val top: Int, val skip: Int)
