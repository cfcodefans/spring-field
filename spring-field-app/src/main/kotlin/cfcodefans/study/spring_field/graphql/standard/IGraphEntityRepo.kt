package cfcodefans.study.spring_field.graphql.standard

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface IGraphEntityRepo : JpaRepository<GraphEntity, Long>, JpaSpecificationExecutor<GraphEntity> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(IGraphEntityRepo::class.java)
    }

    /**
     * Depth-first insert: parent row is saved before any child so [parentId] FKs stay valid.
     * Commits to the file-backed H2 database (not rolled back with test transactions).
     */
    @Transactional
    fun insertTree(root: GraphEntityNode): Int {
        var inserted: Int = 0

        fun persist(node: GraphEntityNode, parentId: Long?) {
            val entity: GraphEntity = node.entity
            entity.parentId = parentId
            val saved: GraphEntity = save(entity)
            log.info("saved ${saved.name}")
            inserted++
            for (child: GraphEntityNode in node.children) {
                persist(child, saved.id)
            }
        }

        persist(root, null)
        return inserted
    }
}
