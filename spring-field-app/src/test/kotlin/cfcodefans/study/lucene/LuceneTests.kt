package cfcodefans.study.lucene

import cfcodefans.study.spring_field.commons.Jsons
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.junit.jupiter.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
open class LuceneTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LuceneTests::class.java)

        /**
         * Converts a Long to a big-endian byte array.
         * NOTE: For sorting numeric values in Lucene, it is more efficient and idiomatic to use
         * `NumericDocValuesField` rather than converting to bytes for a `SortedDocValuesField`.
         */
        fun Long.toBytes(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()

        val ID_GEN: AtomicLong = AtomicLong(0)

        data class LatLonPos(val lat: Double, val lon: Double)

        fun attrToField(key: String, value: Any?): Field? = when (value) {
            is String -> KeywordField(key, value, Field.Store.NO)
            is Int -> IntPoint(key, value)
            is Long -> LongPoint(key, value)
            is Float -> FloatPoint(key, value)
            is Double -> DoublePoint(key, value)
            is Boolean -> StringField(key, value.toString(), Field.Store.NO)
            is LocalDateTime -> value
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
                .let { NumericDocValuesField(key, it) }
            is LatLonPos -> LatLonPoint(key, value.lat, value.lon)
            else -> null
        }

        data class DataEntry(var id: Long = ID_GEN.incrementAndGet(),
                             var name: String = "",
                             var kind: String = "",
                             var createdAt: LocalDateTime = LocalDateTime.now(),
                             var updatedAt: LocalDateTime = LocalDateTime.now(),
                             var tags: Set<String> = emptySet(),
                             var loc: LatLonPos = LatLonPos(0.0, 0.0),
                             var content: String = "",
                             var attrs: MutableMap<String, Any?> = mutableMapOf<String, Any?>()) {
            override fun toString(): String = Jsons.toString(this)

            /**
             * Converts this DataEntry into a Lucene Document using best practices.
             * Each property is mapped to optimal Field types for querying, sorting, and retrieval,
             * using consistent and descriptive field names.
             */
            fun toLuceneDoc(): Document = Document().also { doc: Document ->
                // --- ID ---
                doc.add(StoredField("id", id)) // For retrieval
                doc.add(LongPoint("id_point", id)) // For fast point/range queries
                doc.add(NumericDocValuesField("id_sort", id)) // For efficient numeric sorting

                // --- Name & Kind (Full-text search) ---
                doc.add(TextField("name", name, Field.Store.YES))
                doc.add(TextField("kind", kind, Field.Store.YES))

                // --- Timestamps ---
                val createdEpoch: Long = this.createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()
                doc.add(StoredField("createdAt", createdEpoch))
                doc.add(LongPoint("createdAt_point", createdEpoch))
                doc.add(NumericDocValuesField("createdAt_sort", createdEpoch))

                val updatedEpoch: Long = this.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
                doc.add(StoredField("updatedAt", updatedEpoch))
                doc.add(LongPoint("updatedAt_point", updatedEpoch))
                doc.add(NumericDocValuesField("updatedAt_sort", updatedEpoch))

                // --- Tags (Keyword for filtering, sorting, faceting) ---
                tags.forEach { tag: String -> doc.add(KeywordField("tags", tag, Field.Store.YES)) }

                // --- Location ---
                doc.add(LatLonPoint("loc_point", loc.lat, loc.lon)) // For geo-spatial queries
                doc.add(LatLonDocValuesField("loc_sort", loc.lat, loc.lon)) // For sorting by distance
                doc.add(StoredField("loc_lat", loc.lat)) // For retrieval
                doc.add(StoredField("loc_lon", loc.lon)) // For retrieval

                // --- Content (Full-text search) ---
                doc.add(TextField("content", content, Field.Store.YES))

                // --- Dynamic Attributes ---
                attrs.mapNotNull { (key: String, value: Any?) -> attrToField(key, value) }
                    .forEach { field: Field -> doc.add(field) }
                if (attrs.isNotEmpty()) {
                    doc.add(StoredField("attrs_json", Jsons.toStr(this.attrs)))
                }
            }

            constructor(doc: Document) : this(id = doc.getField("id").numericValue().toLong(),
                                              name = doc.getValues("name").first(),
                                              kind = doc.getValues("kind").first(),
                                              createdAt = doc.getField("createdAt")
                                                  .numericValue()
                                                  .toLong()
                                                  .let { Instant.ofEpochMilli(it) }
                                                  .let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) },
                                              updatedAt = doc.getField("updatedAt")
                                                  .numericValue()
                                                  .toLong()
                                                  .let { Instant.ofEpochMilli(it) }
                                                  .let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) },
                                              tags = doc.getValues("tags").toSet(),
                                              loc = LatLonPos(doc.getField("loc_lat").numericValue().toDouble(),
                                                              doc.getField("loc_lon").numericValue().toDouble()),
                                              content = doc.getValues("content").first(),
                                              attrs = doc.getField("attrs_json")
                                                  ?.stringValue()
                                                  ?.let { Jsons.readToMap(it) }
                                                  ?.toMutableMap()
                                                  ?: mutableMapOf())
        }

    }


    @AutoClose
    val analyzer: StandardAnalyzer = StandardAnalyzer()

    @AutoClose
    val dir: Directory = ByteBuffersDirectory()

    private fun newWriterConfig(): IndexWriterConfig = IndexWriterConfig(analyzer)
    private var controlDataEntries: List<DataEntry> = emptyList()

    @Test
    @Order(0)
    open fun `index mock data and verify count`() {
        controlDataEntries = (LuceneTests::class
            .java
            .getResourceAsStream("/cfcodefans/study/lucene/mock-data-1.json")
            ?: throw IllegalStateException("Cannot find mock-data-1.json in classpath."))
            .use {
                Jsons.read(it.readAllBytes().toString(Charsets.UTF_8),
                           Array<DataEntry>::class.java)
            }.toList()
        val docs: List<Document> = controlDataEntries.map { it.toLuceneDoc() }

        IndexWriter(dir, newWriterConfig())
            .use { writer: IndexWriter ->
                writer.addDocuments(docs)
                log.info("Successfully indexed ${writer.docStats.numDocs} documents.")
                Assertions.assertEquals(controlDataEntries.size, writer.docStats.numDocs.toInt())
            }
    }

    @Test
    @Order(1)
    open fun `fetch all docs from dir`() {
        `index mock data and verify count`()

        DirectoryReader.open(dir).use { reader: DirectoryReader ->
            val searcher: IndexSearcher = IndexSearcher(reader)
            val query: MatchAllDocsQuery = MatchAllDocsQuery()
            // When using MatchAllDocsQuery, it's efficient to ask for reader.maxDoc() hits
            val hits: Int = reader.maxDoc().also { log.info("reader.maxDoc() = $it") }
            val topDocs: TopDocs = searcher.search(query, hits)
            log.info("Successfully fetched ${topDocs.scoreDocs.size} documents.")
            Assertions.assertEquals(controlDataEntries.size, topDocs.scoreDocs.size)
            topDocs.scoreDocs
                .map { scoreDoc: ScoreDoc -> searcher.storedFields().document(scoreDoc.doc) }
                .map { doc: Document -> DataEntry(doc) }
                .toList()
                .also { list ->
                    log.info(list.first().toString())
                }.also { list ->
                    Assertions.assertEquals(controlDataEntries.sortedBy { it.id },
                                            list.sortedBy { it.id })
                }
        }
    }

    @Test
    open fun `test json`() {
        (LuceneTests::class
            .java
            .getResourceAsStream("/cfcodefans/study/lucene/mock-data-1.json")
            ?: throw IllegalStateException("Cannot find mock-data-1.json in classpath."))
            .use {
                Jsons.read(it.readAllBytes().toString(Charsets.UTF_8),
                           Array<DataEntry>::class.java)
            }.let { entries ->
                Assertions.assertTrue(entries.isNotEmpty(), "Should load entries from mock JSON file.")
            }
    }
}