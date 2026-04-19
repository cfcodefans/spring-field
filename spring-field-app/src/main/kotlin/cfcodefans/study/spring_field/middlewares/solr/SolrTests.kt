package cfcodefans.study.spring_field.middlewares.solr

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.request.SolrQuery
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.core.CoreContainer
import org.apache.solr.core.NodeConfig
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class SolrTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SolrTests::class.java)

        const val SOLR_BASE_DIR: String = "./data/solr"

    }

    @BeforeAll
    fun init(): Unit {
        Path.of(SOLR_BASE_DIR)
            .let {
                log.info("Using $SOLR_BASE_DIR")
                if (Files.exists(it).not()) Files.createDirectories(it)
            }
    }

    @Test
    @Order(0)
    fun `prepare schema configuration`(): Unit {
        NodeConfig.NodeConfigBuilder("test_core", Path.of(SOLR_BASE_DIR))
            .setUseSchemaCache(true)
            .setSolrProperties(Properties().apply { put("name", "test_core") })
    }

    @Test
    fun `test embedded solr`(): Unit {
        // 1. Setup Solr Home and Core directories
        val solrHome: Path = Files.createTempDirectory("solr-home")
        val coreName: String = "test_core"
        val coreDir: Path = solrHome.resolve(coreName)
        val confDir: Path = coreDir.resolve("conf")

        var container: CoreContainer? = null
        try {
            // Create directories
            confDir.createDirectories()

            // 2. Write configuration files
            // solr.xml
            solrHome.resolve("solr.xml").writeText("<solr></solr>")

            // core.properties
            coreDir.resolve("core.properties").writeText("name=$coreName")

            // solrconfig.xml (Minimal)
            confDir.resolve("solrconfig.xml").writeText("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <config>
                    <luceneMatchVersion>9.7</luceneMatchVersion>
                    <directoryFactory name="DirectoryFactory" class="${'$'}{solr.directoryFactory:solr.NRTCachingDirectoryFactory}"/>
                    <schemaFactory class="ClassicIndexSchemaFactory"/>
                    <requestHandler name="/select" class="solr.SearchHandler"/>
                    <requestHandler name="/update" class="solr.UpdateRequestHandler"/>
                </config>
            """.trimIndent())

            // schema.xml (Minimal)
            confDir.resolve("schema.xml").writeText("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <schema name="minimal-schema" version="1.6">
                    <field name="id" type="string" indexed="true" stored="true" required="true" multiValued="false" />
                    <field name="content" type="text_general" indexed="true" stored="true" />
                    <field name="_version_" type="plong" indexed="true" stored="true"/>
                    
                    <uniqueKey>id</uniqueKey>

                    <fieldType name="string" class="solr.StrField" sortMissingLast="true" />
                    <fieldType name="plong" class="solr.LongPointField" docValues="true"/>
                    <fieldType name="text_general" class="solr.TextField" positionIncrementGap="100">
                        <analyzer>
                            <tokenizer class="solr.StandardTokenizerFactory"/>
                            <filter class="solr.LowerCaseFilterFactory"/>
                        </analyzer>
                    </fieldType>
                </schema>
            """.trimIndent())

            // 3. Initialize CoreContainer
            container = CoreContainer(solrHome, Properties())
            container.load()

            // 4. Create EmbeddedSolrServer
            val solrServer: EmbeddedSolrServer = EmbeddedSolrServer(container, coreName)

            // 5. Index a document
            val doc: SolrInputDocument = SolrInputDocument()
            doc.addField("id", "doc1")
            doc.addField("content", "Hello Embedded Solr in Kotlin")
            solrServer.add(doc)
            solrServer.commit()

            // 6. Query
            val query = SolrQuery("*:*")
            val response = solrServer.query(query)

            println("Results found: " + response.results.numFound)
            for (solrDocument in response.results) {
                println(solrDocument)
            }

            solrServer.close()
        } finally {
            if (container != null && !container.isShutDown) {
                container.shutdown()
            }
            // cleanup temp dir
            solrHome.toFile().deleteRecursively()
        }
    }
}