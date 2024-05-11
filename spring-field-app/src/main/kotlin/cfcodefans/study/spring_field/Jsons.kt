package com.thenetcircle.commons

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.thenetcircle.commons.DateTimeHelper.DEFAULT_DATE_TIME_FORMAT
import com.thenetcircle.commons.DateTimeHelper.toLocalDateTime
import org.apache.commons.lang3.time.DateUtils
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object Jsons {
    // https://www.baeldung.com/jackson-kotlin
    val MAPPER: ObjectMapper = jacksonObjectMapper().apply {
        configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
        configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Ignore Null Fields Globally
        //MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // SerializationFeature.INDENT_OUTPUT = Globally Pretty Printer
        // Please don't set it Globally, ActivityStreams's content have to use the DefaultPrettyPrinter
        // {
        //    "name" : "mkyong",
        //    "age" : 38,
        //    "skills" : [ "java", "python", "node", "kotlin" ]
        // }
        //MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
        configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        dateFormat = SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT)
        registerModule(JavaTimeModule())
    }

    fun read(input: InputStream?): JsonNode {
        requireNotNull(input) { "reading json however the input stream is empty" }
        try {
            return MAPPER.readValue(input, JsonNode::class.java)
        } catch (e: Exception) {
            throw RuntimeException("reading json stream", e)
        }
    }

    fun read(raw: String?): JsonNode {
        requireNotNull(raw) { "reading json however the json raw is empty" }
        try {
            return MAPPER.readValue(raw, JsonNode::class.java)
        } catch (e: Exception) {
            throw RuntimeException("reading json raw", e)
        }
    }

    fun <T> read(raw: String?, cls: Class<T>): T {
        requireNotNull(raw) { "reading json however the json raw is empty" }
        try {
            return MAPPER.readValue(raw, cls)
        } catch (e: Exception) {
            throw RuntimeException("reading json raw", e)
        }
    }

    fun <T> read(jn: JsonNode?, cls: Class<T>): T {
        requireNotNull(jn) { "reading json however the json node is null" }
        try {
            return MAPPER.readValue(jn.traverse(), cls)
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

    // pretty printer
    // {
    //    "name" : "Dean",
    //    "age" : 38,
    //    "skills" : [ "java", "python", "node", "kotlin" ]
    // }
    fun toString(obj: Any): String = try {
        MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    } catch (e: Exception) {
        throw RuntimeException("deserialize json to string", e)
    }

    // default print
    // {"name" : "Dean","age" : 38,"skills" : [ "java", "python", "node", "kotlin" ]}
    fun toStringWithoutPrettyPrinter(obj: Any): String = try {
        MAPPER.writeValueAsString(obj)
    } catch (e: Exception) {
        throw RuntimeException("deserialize json to string", e)
    }

    fun toNode(any: Any?): JsonNode = try {
        //TODO
        if (any == null)
            MAPPER.nullNode()
        else
            MAPPER.convertValue(any,
                when (any) {
                    is Boolean -> BooleanNode::class.java
                    is Char -> TextNode::class.java
                    is String -> TextNode::class.java
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
        val jn = MAPPER.createObjectNode()
        map.forEach { (k, v) -> jn.putPOJO(k, v) }
        jn
    } catch (e: Exception) {
        throw RuntimeException("serialize Map to json\n\t${map}", e)
    }

    const val ISO_DATE_SCHEMA: String = "iso:date://"

//    fun normalize(param: Any?): Any? = when {
//        param !is String -> param
//        param.startsWith(ISO_DATE_SCHEMA) -> DateUtils.parseDate(param.substringAfter(ISO_DATE_SCHEMA).trim(), JS_ISO_DATETIME_FORMAT).toLocalDateTime()
//        else -> param
//    }

    fun <T> readOrNull(raw: String?, cls: Class<T>): T? = if (raw.isNullOrBlank())
        null
    else
        runCatching { MAPPER.readValue(raw, cls) }.getOrNull()

    fun <T> readOrNull(jn: JsonNode?, cls: Class<T>): T? = jn
        ?.let {
            runCatching { MAPPER.readValue(it.traverse(), cls) }.getOrNull()
        }

    const val JS_ISO_DATETIME_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun <T : JsonNode> ObjectNode.getOrDefault(key: String, default: T?): T? {
        val re = this.get(key)
        return if (re == null) {
            this.set<T>(key, default)
            default
        } else
            re as T
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
        is Date -> MAPPER.dateFormat.format(value)
        is Any -> "\"$value\""
        else -> "null"
    }
}