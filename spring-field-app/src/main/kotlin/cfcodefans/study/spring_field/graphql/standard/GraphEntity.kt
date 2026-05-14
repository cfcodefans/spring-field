package cfcodefans.study.spring_field.graphql.standard

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * Generic graph-style entity: hierarchical nodes with JSON payloads and tags.
 * `entityType` maps the requested "type" classification (node, edge, person, document, …).
 */
@Entity
@Table(name = "graph_entity")
@EntityListeners(AuditingEntityListener::class)
open class GraphEntity(@Id
                       @GeneratedValue(strategy = GenerationType.IDENTITY)
                       open var id: Long = 0,

                       @Column(name = "entity_type", nullable = false, length = 64)
                       open var entityType: String = "",

                       @Column(nullable = false, length = 512)
                       open var name: String = "",

                       @Column(name = "parent_id")
                       open var parentId: Long? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "data")
                       open var data: JsonNode? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "note")
                       open var note: JsonNode? = null,

                       @JdbcTypeCode(SqlTypes.JSON)
                       @Column(name = "tags", nullable = false)
                       open var tags: MutableList<String> = mutableListOf(),

                       @CreatedDate
                       @Column(name = "created_at", nullable = false, updatable = false)
                       open var createdAt: Instant = Instant.now(),

                       @LastModifiedDate
                       @Column(name = "updated_at", nullable = false)
                       open var updatedAt: Instant = Instant.now())
