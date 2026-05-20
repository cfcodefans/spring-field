package cfcodefans.study.spring_field.api.query.standard

import cfcodefans.study.spring_field.api.query.GraphQLWebApp
import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [GraphQLWebApp::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("graphql-web")
class StandardGraphQlApiTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `POST graphql returns seeded entities`() {
        val body: String = """{"query":"query { entities { id entityType name } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()")
                           .value(greaterThan(0)))
            .andExpect(jsonPath("$.data.entities[0].entityType").exists())
    }

    @Test
    fun `POST graphql entities filter by entityType`() {
        val body: String = """{"query":"query { entities(filter: { entityType: \"person\" }) { id name } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities[0].name")
                           .value("Ada"))
    }

    @Test
    fun `POST graphql entities filter rootOnly and nameContains`() {
        val body: String =
            "{\"query\":\"query { entities(filter: { rootOnly: true, nameContains: \\\"node\\\" }) { name entityType } }\"}"
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()").value(1))
            .andExpect(jsonPath("$.data.entities[0].name").value("Orphan node"))
    }

    @Test
    fun `POST graphql entities filter entityTypeIn`() {
        val body: String =
            """{"query":"query { entities(filter: { entityTypeIn: [\"person\", \"node\"] }) { name entityType } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()").value(2))
    }

    @Test
    fun `POST graphql entities filter by id and entityTypeIn with AND`() {
        val listBody: String = """{"query":"query { entities { id name } }"}"""
        val listJson: String = mockMvc.perform(post("/graphql")
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(listBody))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val adaId: String = JsonPath.read(listJson, "$.data.entities[?(@.name == 'Ada')].id[0]")
        val filterBody: String =
            """{"query":"query { entities(filter: { id: \"$adaId\", entityTypeIn: [\"person\", \"document\"] }) { id name entityType } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(filterBody))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()").value(1))
            .andExpect(jsonPath("$.data.entities[0].name").value("Ada"))
            .andExpect(jsonPath("$.data.entities[0].entityType").value("person"))
    }

    @Test
    fun `POST graphql entities filter dataLike noteLike and updatedAtBetween`() {
        val body: String =
            "{\"query\":\"query { entities(filter: { entityType: \\\"person\\\", dataLike: \\\"%architect%\\\", noteLike: \\\"%seed%\\\", updatedAtBetween: { from: \\\"1970-01-01T00:00:00Z\\\", to: \\\"2099-12-31T23:59:59Z\\\" } }) { name data note } }\"}"
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()").value(1))
            .andExpect(jsonPath("$.data.entities[0].name").value("Ada"))
    }
}
