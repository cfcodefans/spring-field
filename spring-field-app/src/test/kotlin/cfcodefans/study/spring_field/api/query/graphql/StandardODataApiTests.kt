package cfcodefans.study.spring_field.api.query.graphql

import cfcodefans.study.spring_field.api.query.GraphQLWebApp
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [GraphQLWebApp::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("graphql-web")
class StandardODataApiTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET odata metadata returns CSDL xml`() {
        mockMvc.perform(get("/odata/v4/\$metadata").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("GraphEntities")))
    }

    @Test
    fun `GET GraphEntities with filter eq`() {
        mockMvc.perform(
            get("/odata/v4/GraphEntities")
                .param("\$filter", "entityType eq 'file'")
                .param("\$top", "5"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value.length()").value(greaterThan(0)))
            .andExpect(jsonPath("$.value[0].entityType").value("file"))
    }

    @Test
    fun `GET GraphEntities with select and contains`() {
        mockMvc.perform(
            get("/odata/v4/GraphEntities")
                .param("\$filter", "contains(name,'Repo')")
                .param("\$select", "id,name,entityType")
                .param("\$top", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value.length()").value(greaterThan(0)))
            .andExpect(jsonPath("$.value[0].name").exists())
            .andExpect(jsonPath("$.value[0].data").doesNotExist())
    }

    @Test
    fun `GET GraphEntities by id`() {
        val listJson: String = mockMvc.perform(get("/odata/v4/GraphEntities").param("\$top", "1"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val id: String = Regex(""""id"\s*:\s*(\d+)""")
            .find(listJson)!!
            .groupValues[1]
        mockMvc.perform(get("/odata/v4/GraphEntities($id)"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toInt()))
    }
}
