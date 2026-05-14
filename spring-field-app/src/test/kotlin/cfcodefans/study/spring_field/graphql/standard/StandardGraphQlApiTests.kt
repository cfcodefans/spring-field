package cfcodefans.study.spring_field.graphql.standard

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
    fun `POST graphql entitiesByType filters`() {
        val body: String = """{"query":"query { entitiesByType(entityType: \"person\") { id name } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entitiesByType[0].name")
                           .value("Ada"))
    }
}