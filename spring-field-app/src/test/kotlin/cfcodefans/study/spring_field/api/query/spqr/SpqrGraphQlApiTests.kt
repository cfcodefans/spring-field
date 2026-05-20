package cfcodefans.study.spring_field.api.query.spqr

import cfcodefans.study.spring_field.api.query.GraphQLWebApp
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [GraphQLWebApp::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("graphql-spqr-web")
open class SpqrGraphQlApiTests {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SpqrGraphQlApiTests::class.java)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET graphql schema returns SDL text`() {
        mockMvc.perform(get("/graphql/schema").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("type Query")))
    }

    @Test
    fun `POST graphql returns seeded entities`() {
        val body: String = """{"query":"query { entities { id entityType name } }"}"""
        mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
            .also { re -> re.andReturn().response.toString().let { log.info(it) } }
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.entities.length()")
                           .value(greaterThan(0)))
    }
}