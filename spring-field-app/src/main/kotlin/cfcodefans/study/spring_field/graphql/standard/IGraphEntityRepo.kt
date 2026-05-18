package cfcodefans.study.spring_field.graphql.standard

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface IGraphEntityRepo : JpaRepository<GraphEntity, Long>, JpaSpecificationExecutor<GraphEntity>
