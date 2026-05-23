package cfcodefans.study.spring_field.api.query.spring_filter

import cfcodefans.study.spring_field.api.query.GraphEntity
import cfcodefans.study.spring_field.api.query.IGraphEntityRepo
//import com.turkraft.springfilter.boot.Fields
//import com.turkraft.springfilter.boot.Filter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/spring-filter/")
open class SpringFilterQueryCtrl(private val repo: IGraphEntityRepo) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SpringFilterQueryCtrl::class.java)
    }

    @GetMapping
    @Operation(summary = "Dynamic entity search engine for AI Bot queries",
               description = "Provides AST mapping using spring-filter over all core paths. Supports a lightweight text fallback over native raw JSON blocks.")
//    @Fields
    open fun searchEntities(
            // Automatically injects openapi-documented AST parsing from the "filter" URL parameter
//            @Filter(entityClass = GraphEntity::class)
            filterSpec: Specification<GraphEntity>?,

            // Recovers raw URL string parameter to enforce security constraints
            @Parameter(description = "Raw expression query string to analyze structural safety")
            @RequestParam(value = "filter", required = false) rawFilterString: String?,

            // Handles dynamic Bot sorting configurations and sliding chunk size limits
            pageable: Pageable): List<GraphEntity> {

        // -----------------------------------------------------------------
        // MANDATORY ARCHITECTURAL FIREWALL
        // -----------------------------------------------------------------
        if (!rawFilterString.isNullOrBlank()) {
            val targetedQueryLower: String = rawFilterString.lowercase()

            // Database Overload Guardrail: If scanning raw data/note blocks via fuzzy patterns,
            // the bot MUST supply a specialized b-tree indexed column to slice the dataset down.
            if ((targetedQueryLower.contains("data") || targetedQueryLower.contains("note")) &&
                !targetedQueryLower.contains("entitytype") && !targetedQueryLower.contains("id")
            ) {
                throw IllegalArgumentException("Security Protection: Running string scanning evaluations over schemaless JSON blocks " +
                                                       "requires an indexed prefix restraint like 'entityType' to prevent global table scans.")
            }
        }

        // -----------------------------------------------------------------
        // RUNTIME EXECUTION
        // -----------------------------------------------------------------
        val finalSpecification: Specification<GraphEntity> = filterSpec ?: Specification.unrestricted()

        // Return matching rows inside safe sliding boundaries (Maximum page boundaries can be enforced here)
        return repo.findAll(finalSpecification, pageable).content
    }
}