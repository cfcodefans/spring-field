package cfcodefans.study.spring_field.graphql.standard

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GraphEntityRepo : JpaRepository<GraphEntity, Long> {
    fun findByEntityType(entityType: String): List<GraphEntity>
    fun findByParentIdIsNull(): List<GraphEntity>
    fun findByParentId(parentId: Long): List<GraphEntity>
}
