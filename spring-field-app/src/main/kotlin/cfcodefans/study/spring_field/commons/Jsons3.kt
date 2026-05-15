package cfcodefans.study.spring_field.commons

import cfcodefans.study.spring_field.commons.Jsons3.fakeLiteral
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.apache.commons.lang3.time.DateUtils
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonReadFeature
import tools.jackson.core.json.JsonWriteFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.*
import tools.jackson.module.kotlin.KotlinModule
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object Jsons3 {

    const val DEFAULT_DATE_TIME_FORMAT: String = "yyyy-MM-dd HH:mm:ss"

    /** Used where we format dates outside the mapper (e.g. [fakeLiteral]). */
    private val displayDateFormat: DateFormat = SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT)

    // https://www.baeldung.com/jackson-kotlin — Jackson 3: tools.jackson + JsonMapper builder
    val MAPPER: ObjectMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .enable(StreamReadFeature.AUTO_CLOSE_SOURCE)
        .enable(StreamReadFeature.IGNORE_UNDEFINED)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES,
                JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS,
                JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonWriteFeature.QUOTE_PROPERTY_NAMES, true)
        .configure(JsonWriteFeature.ESCAPE_NON_ASCII, false)
        .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .defaultDateFormat(SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT))
        .build()

    fun read(input: InputStream?): JsonNode {
        requireNotNull(input) { "reading json however the input stream is empty" }
        return try {
            MAPPER.readValue(input, JsonNode::class.java)
        } catch (e: Exception) {
            throw RuntimeException("reading json stream", e)
        }
    }

    fun read(raw: String?): JsonNode {
        requireNotNull(raw) { "reading json however the json raw is empty" }
        return try {
            MAPPER.readValue(raw, JsonNode::class.java)
        } catch (e: Exception) {
            throw RuntimeException("reading json raw", e)
        }
    }

    fun <T> read(raw: String?, cls: Class<T>): T {
        requireNotNull(raw) { "reading json however the json raw is empty" }
        return try {
            MAPPER.readValue(raw, cls)
        } catch (e: Exception) {
            throw RuntimeException("reading json raw", e)
        }
    }

    fun <T> read(jn: JsonNode?, cls: Class<T>): T {
        requireNotNull(jn) { "reading json however the json node is null" }
        return try {
            MAPPER.readValue(MAPPER.treeAsTokens(jn), cls)
        } catch (e: Exception) {
            throw RuntimeException("reading json node to ${cls.name}", e)
        }
    }

    val MAP_TYPE_REF = object : TypeReference<Map<String, Any?>?>() {}

    fun readToMap(raw: String?): Map<String, Any?>? {
        requireNotNull(raw) { "reading json however the json raw is empty" }
        return try {
            MAPPER.readValue(raw, MAP_TYPE_REF)
        } catch (e: Exception) {
            throw RuntimeException("reading json raw", e)
        }
    }

    /** pretty printer
    <pre> {
    "name" : "Dean",
    "age" : 38,
    "skills" : [ "java", "python", "node", "kotlin" ]
    } </pre>
     **/
    fun toString(obj: Any?): String = try {
        MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    } catch (e: Exception) {
        throw RuntimeException("deserialize json to string", e)
    }

    // default print
    // {"name" : "Dean","age" : 38,"skills" : [ "java", "python", "node", "kotlin" ]}
    fun toStr(obj: Any): String = try {
        MAPPER.writeValueAsString(obj)
    } catch (e: Exception) {
        throw RuntimeException("deserialize json to string", e)
    }

    fun toNode(any: Any?): JsonNode = try {
        if (any == null) MAPPER.nullNode()
        else MAPPER.convertValue(any,
                                 when (any) {
                                     is Boolean -> BooleanNode::class.java
                                     is Char -> StringNode::class.java
                                     is String -> StringNode::class.java
                                     is Number -> DecimalNode::class.java
                                     is Map<*, *> -> POJONode::class.java
                                     else -> ObjectNode::class.java
                                 })
    } catch (e: Exception) {
        throw RuntimeException("serialize object to json\n\t${any}", e)
    }

    fun jsonOrNull(raw: String?): JsonNode? = if (raw.isNullOrBlank())
        null
    else
        runCatching { MAPPER.readValue(raw, JsonNode::class.java) }.getOrNull()

    fun toJson(map: Map<String, Any?>): JsonNode = try {
        MAPPER.createObjectNode()
            .also { jn -> map.forEach { (k, v) -> jn.putPOJO(k, v) } }
    } catch (e: Exception) {
        throw RuntimeException("serialize Map to json\n\t${map}", e)
    }

    const val ISO_DATE_SCHEMA: String = "iso:date://"

    fun normalize(param: Any?): Any? = when {
        param !is String -> param
        param.startsWith(ISO_DATE_SCHEMA) -> DateUtils.parseDate(param.substringAfter(ISO_DATE_SCHEMA).trim(), JS_ISO_DATETIME_FORMAT)//.toLDT()
        else -> param
    }

    fun <T> readOrNull(raw: String?, cls: Class<T>): T? = if (raw.isNullOrBlank()) null
    else runCatching { MAPPER.readValue(raw, cls) }.getOrNull()

    fun <T> readOrNull(jn: JsonNode?, cls: Class<T>): T? = jn
        ?.let { runCatching { MAPPER.readValue(MAPPER.treeAsTokens(it), cls) }.getOrNull() }

    const val JS_ISO_DATETIME_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun <T : JsonNode> ObjectNode.getOrDefault(key: String, default: T?): T? = this.get(key)
        ?.let { it as T }
        ?: default.also { v ->
            if (v == null) putNull(key)
            else set(key, v)
        }

    /**
     * intend to resolve some escaped chars, doesn't work well
     */
    fun fakeJson(map: Map<String, Any?>): String = """
        {${
        map.entries.joinToString(",\n\t") { en -> "\"${en.key}\": ${fakeLiteral(en.value)}" }
    }}""".trimIndent()

    fun fakeLiteral(value: Any?): String = when (value) {
        is Number -> value.toString()
        is Array<*> -> value.joinToString(", ") { fakeLiteral(it) }
        is Collection<*> -> value.joinToString(", ") { fakeLiteral(it) }
        is Date -> displayDateFormat.format(value)
        is Any -> "\"$value\""
        else -> "null"
    }

    fun toStringWithoutPrettyPrinter(obj: Any?): String = try {
        MAPPER.writeValueAsString(obj)
    } catch (e: Exception) {
        throw RuntimeException("deserialize json to string", e)
    }
}

@Converter(autoApply = false)
open class ObjectNodeConverter : AttributeConverter<ObjectNode?, String?> {
    override fun convertToDatabaseColumn(attribute: ObjectNode?): String? = attribute?.toString()

    override fun convertToEntityAttribute(dbData: String?): ObjectNode? = dbData
        ?.ifBlank { null }
        ?.let { Jsons3.read(dbData, ObjectNode::class.java) }
}
