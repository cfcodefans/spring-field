package cfcodefans.study.spring_field.api.query.odata

import cfcodefans.study.spring_field.api.query.GraphEntity
import jakarta.persistence.criteria.*
import jakarta.persistence.criteria.Expression
import org.apache.olingo.commons.api.edm.EdmEnumType
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind
import org.apache.olingo.commons.api.edm.EdmType
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty
import org.apache.olingo.server.api.uri.queryoption.expression.*
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import org.apache.olingo.server.api.uri.queryoption.expression.Expression as OdataExpression

/**
 * Maps OData $filter AST nodes to JPA [Specification] predicates (allowlisted properties only).
 */
internal class ODataFilterSpecificationVisitor : ExpressionVisitor<FilterExpr> {
    override fun visitBinaryOperator(operator: BinaryOperatorKind, left: FilterExpr, right: FilterExpr): FilterExpr =
        when (operator) {
            BinaryOperatorKind.AND -> FilterExpr.Combined(left.toSpecification().and(right.toSpecification()))
            BinaryOperatorKind.OR -> FilterExpr.Combined(left.toSpecification().or(right.toSpecification()))
            BinaryOperatorKind.EQ, BinaryOperatorKind.NE,
            BinaryOperatorKind.GT, BinaryOperatorKind.GE,
            BinaryOperatorKind.LT, BinaryOperatorKind.LE,
            -> comparisonSpec(operator, left, right)
            BinaryOperatorKind.IN -> inSpec(left, right)
            else -> throw IllegalArgumentException("unsupported binary operator: $operator")
        }

    override fun visitBinaryOperator(operator: BinaryOperatorKind,
                                     left: FilterExpr,
                                     right: MutableList<FilterExpr>): FilterExpr =
        when (operator) {
            BinaryOperatorKind.IN -> inSpec(left, FilterExpr.Value(right.map { expr: FilterExpr -> expr.requireValue() }))
            else -> throw IllegalArgumentException("unsupported binary operator with list: $operator")
        }

    override fun visitUnaryOperator(operator: UnaryOperatorKind, operand: FilterExpr): FilterExpr =
        when (operator) {
            UnaryOperatorKind.NOT -> FilterExpr.Combined(
                Specification { root, query, cb ->
                    cb.not(operand.toSpecification().toPredicate(root, query, cb))
                },
            )
            UnaryOperatorKind.MINUS -> throw IllegalArgumentException("unary minus is not supported")
            else -> throw IllegalArgumentException("unsupported unary operator: $operator")
        }

    override fun visitMethodCall(methodCall: MethodKind, parameters: MutableList<FilterExpr>): FilterExpr {
        if (parameters.size < 2) {
            throw IllegalArgumentException("method $methodCall requires at least two parameters")
        }
        val property: String = parameters[0].requireProperty()
        val raw: String = parameters[1].requireLiteralAsString()
        val pattern: String = when (methodCall) {
            MethodKind.CONTAINS -> "%${escapeLike(raw.lowercase())}%"
            MethodKind.STARTSWITH -> "${escapeLike(raw.lowercase())}%"
            MethodKind.ENDSWITH -> "%${escapeLike(raw.lowercase())}"
            else -> throw IllegalArgumentException("unsupported method: $methodCall")
        }
        return likeProperty(property, pattern)
    }

    override fun visitLambdaExpression(lambdaFunction: String,
                                       lambdaVariable: String,
                                       expression: OdataExpression): FilterExpr =
        throw IllegalArgumentException("lambda expressions are not supported")

    override fun visitLiteral(literal: Literal): FilterExpr = FilterExpr.Value(parseLiteral(literal))

    override fun visitMember(member: Member): FilterExpr = FilterExpr.Property(memberPropertyName(member))

    override fun visitAlias(aliasName: String): FilterExpr =
        throw IllegalArgumentException("aliases are not supported")

    override fun visitTypeLiteral(type: EdmType): FilterExpr =
        throw IllegalArgumentException("type literals are not supported")

    override fun visitLambdaReference(variableName: String): FilterExpr =
        throw IllegalArgumentException("lambda references are not supported")

    override fun visitEnum(type: EdmEnumType, enumValues: MutableList<String>): FilterExpr =
        FilterExpr.Value(enumValues.firstOrNull())

    companion object {
        private val ALLOWED_PROPERTIES: Set<String> = setOf(
                "id", "entityType", "name", "parentId", "data", "note", "createdAt", "updatedAt",
        )

        fun toSpecification(expression: OdataExpression?): Specification<GraphEntity> {
            if (expression == null) {
                return unrestricted()
            }
            val expr: FilterExpr = expression.accept(ODataFilterSpecificationVisitor())
            return expr.toSpecification()
        }

        private fun unrestricted(): Specification<GraphEntity> =
            Specification { _, _, cb -> cb.conjunction() }

        private fun memberPropertyName(member: Member): String {
            val last = member.resourcePath.uriResourceParts.last()
            val property: String = when (last) {
                is UriResourcePrimitiveProperty -> last.property.name
                else -> throw IllegalArgumentException("unsupported member resource: $last")
            }
            require(ALLOWED_PROPERTIES.contains(property)) { "filter property not allowed: $property" }
            return property
        }

        private fun comparisonSpec(operator: BinaryOperatorKind, left: FilterExpr, right: FilterExpr): FilterExpr {
            val property: String = left.requireProperty()
            val value: Any? = right.requireValue()
            return FilterExpr.Combined(Specification { root, _, cb ->
                val path: Path<Any> = typedPath(root, property)
                val literal: Any? = coerce(property, value)
                when (operator) {
                    BinaryOperatorKind.EQ -> cb.equal(path, literal)
                    BinaryOperatorKind.NE -> cb.notEqual(path, literal)
                    BinaryOperatorKind.GT -> greaterThan(cb, path, literal)
                    BinaryOperatorKind.GE -> greaterThanOrEqual(cb, path, literal)
                    BinaryOperatorKind.LT -> lessThan(cb, path, literal)
                    BinaryOperatorKind.LE -> lessThanOrEqual(cb, path, literal)
                    else -> throw IllegalArgumentException("unsupported operator: $operator")
                }
            })
        }

        private fun inSpec(left: FilterExpr, right: FilterExpr): FilterExpr {
            val property: String = left.requireProperty()
            val values: List<Any?> = right.requireValueList()
            return FilterExpr.Combined(Specification { root, _, cb ->
                val path: Path<Any> = typedPath(root, property)
                path.`in`(values.map { value: Any? -> coerce(property, value) })
            })
        }

        private fun likeProperty(property: String, pattern: String): FilterExpr =
            FilterExpr.Combined(Specification { root, _, cb ->
                when (property) {
                    "name", "entityType" -> {
                        val path: Expression<String> = cb.lower(root.get(property))
                        cb.like(path, pattern, '\\')
                    }
                    "data", "note" -> {
                        val jsonAsString: Expression<String> = cb.lower(
                            root.get<Any>(property).`as`(String::class.java),
                        )
                        cb.like(jsonAsString, pattern, '\\')
                    }
                    else -> throw IllegalArgumentException("LIKE not supported on property: $property")
                }
            })

        private fun greaterThan(cb: CriteriaBuilder, path: Path<Any>, literal: Any?): Predicate =
            when (literal) {
                is Instant -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThan(path as Path<Instant>, literal)
                is Long -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThan(path as Path<Long>, literal)
                is String -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThan(path as Path<String>, literal)
                else -> throw IllegalArgumentException("unsupported comparison type: ${literal?.javaClass}")
            }

        private fun greaterThanOrEqual(cb: CriteriaBuilder, path: Path<Any>, literal: Any?): Predicate =
            when (literal) {
                is Instant -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThanOrEqualTo(path as Path<Instant>, literal)
                is Long -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThanOrEqualTo(path as Path<Long>, literal)
                is String -> @Suppress("UNCHECKED_CAST")
                    cb.greaterThanOrEqualTo(path as Path<String>, literal)
                else -> throw IllegalArgumentException("unsupported comparison type: ${literal?.javaClass}")
            }

        private fun lessThan(cb: CriteriaBuilder, path: Path<Any>, literal: Any?): Predicate =
            when (literal) {
                is Instant -> @Suppress("UNCHECKED_CAST")
                    cb.lessThan(path as Path<Instant>, literal)
                is Long -> @Suppress("UNCHECKED_CAST")
                    cb.lessThan(path as Path<Long>, literal)
                is String -> @Suppress("UNCHECKED_CAST")
                    cb.lessThan(path as Path<String>, literal)
                else -> throw IllegalArgumentException("unsupported comparison type: ${literal?.javaClass}")
            }

        private fun lessThanOrEqual(cb: CriteriaBuilder, path: Path<Any>, literal: Any?): Predicate =
            when (literal) {
                is Instant -> @Suppress("UNCHECKED_CAST")
                    cb.lessThanOrEqualTo(path as Path<Instant>, literal)
                is Long -> @Suppress("UNCHECKED_CAST")
                    cb.lessThanOrEqualTo(path as Path<Long>, literal)
                is String -> @Suppress("UNCHECKED_CAST")
                    cb.lessThanOrEqualTo(path as Path<String>, literal)
                else -> throw IllegalArgumentException("unsupported comparison type: ${literal?.javaClass}")
            }

        private fun typedPath(root: Root<GraphEntity>, property: String): Path<Any> =
            @Suppress("UNCHECKED_CAST")
            root.get<Any>(property) as Path<Any>

        private fun coerce(property: String, value: Any?): Any? = when (property) {
            "id", "parentId" -> when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> value
            }
            "createdAt", "updatedAt" -> when (value) {
                is Instant -> value
                is String -> parseInstant(value)
                else -> value
            }
            else -> value
        }

        private fun parseLiteral(literal: Literal): Any? {
            if (literal.text == "null") return null
            val typeName: String = literal.type.fullQualifiedName.name
            return when (typeName) {
                EdmPrimitiveTypeKind.String.name -> literal.text
                EdmPrimitiveTypeKind.Int64.name -> literal.text.toLong()
                EdmPrimitiveTypeKind.Int32.name -> literal.text.toInt()
                EdmPrimitiveTypeKind.Boolean.name -> literal.text.toBoolean()
                EdmPrimitiveTypeKind.DateTimeOffset.name -> parseInstant(literal.text)
                else -> if (literal.type is EdmEnumType) literal.text else literal.text
            }
        }

        private fun parseInstant(text: String): Instant = try {
            Instant.parse(text)
        } catch (_: DateTimeParseException) {
            OffsetDateTime.parse(text).toInstant()
        }

        private fun escapeLike(needle: String): String = needle
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}

internal sealed interface FilterExpr {
    fun toSpecification(): Specification<GraphEntity>

    fun requireProperty(): String = when (this) {
        is Property -> name
        else -> throw IllegalArgumentException("expected property, got $this")
    }

    fun requireValue(): Any? = when (this) {
        is Value -> value
        else -> throw IllegalArgumentException("expected literal, got $this")
    }

    fun requireLiteralAsString(): String = requireValue()?.toString()
        ?: throw IllegalArgumentException("expected string literal")

    fun requireValueList(): List<Any?> = when (this) {
        is Value -> when (val v: Any? = value) {
            is List<*> -> v.map { item: Any? -> item }
            else -> listOf(v)
        }
        else -> throw IllegalArgumentException("expected literal list, got $this")
    }

    data class Property(val name: String) : FilterExpr {
        override fun toSpecification(): Specification<GraphEntity> =
            throw IllegalArgumentException("property $name cannot be used alone")
    }

    data class Value(val value: Any?) : FilterExpr {
        override fun toSpecification(): Specification<GraphEntity> =
            throw IllegalArgumentException("literal $value cannot be used alone")
    }

    data class Combined(val spec: Specification<GraphEntity>) : FilterExpr {
        override fun toSpecification(): Specification<GraphEntity> = spec
    }
}
