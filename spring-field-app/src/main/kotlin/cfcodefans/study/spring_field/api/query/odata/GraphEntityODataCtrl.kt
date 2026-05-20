package cfcodefans.study.spring_field.api.query.odata

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * OData v4-style demo over [cfcodefans.study.spring_field.graphql.GraphEntity] / [IGraphEntityRepo].
 *
 * Examples (profile `graphql-web`):
 * - GET /odata/v4/$metadata
 * - GET /odata/v4/GraphEntities?$filter=entityType eq 'file'&$top=10
 * - GET /odata/v4/GraphEntities?$filter=contains(name,'Repo')&$select=id,name,entityType
 * - GET /odata/v4/GraphEntities(1)
 */
@RestController
@RequestMapping("/odata/v4")
open class GraphEntityODataCtrl(private val odataService: ODataGraphEntityService) {
    @GetMapping("/\$metadata", produces = [MediaType.APPLICATION_XML_VALUE])
    open fun metadata(): ResponseEntity<String> = ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .body(odataService.metadataXml())

    @GetMapping("/GraphEntities", produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun graphEntities(request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        val result: ODataEntitySetResult = odataService.queryEntitySet(resourcePath = "GraphEntities",
                                                                       rawQuery = request.queryString)
        val body: MutableMap<String, Any?> = linkedMapOf("@odata.context" to result.context,
                                                         "value" to result.value)
        result.count?.let { count: Long -> body["@odata.count"] = count }
        return ResponseEntity.ok(body)
    }

    @GetMapping("/GraphEntities({id})", produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun graphEntityById(@PathVariable id: Long, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        val result: ODataEntityResult = odataService.queryEntityByKey(resourcePath = "GraphEntities($id)",
                                                                      rawQuery = request.queryString)
        val body: LinkedHashMap<String, Any?> = linkedMapOf("@odata.context" to result.context)
        body.putAll(result.entity)
        return ResponseEntity.ok(body)
    }
}
