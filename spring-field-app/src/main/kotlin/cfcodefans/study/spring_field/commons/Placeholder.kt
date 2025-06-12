package cfcodefans.study.spring_field.commons

import java.io.Serializable
import java.util.*

open class Placeholder(val delimiterHead: String,
                       val delimiterTail: String) : Serializable {
    companion object {
        private fun hashCode(head: String?, tail: String?): Int = Objects.hash(head, tail)

        private val PLACEHOLDERS = HashMap<Int, Placeholder>()

        operator fun get(aDelimiterHead: String, aDelimiterTail: String): Placeholder =
            PLACEHOLDERS.computeIfAbsent(hashCode(aDelimiterHead, aDelimiterTail))
            { Placeholder(aDelimiterHead, aDelimiterTail) }
    }

    override fun equals(other: Any?): Boolean = (this === other)
            || (other is Placeholder
            && Objects.equals(delimiterHead, other.delimiterHead)
            && Objects.equals(delimiterTail, other.delimiterTail))

    override fun hashCode(): Int = hashCode(this.delimiterHead, this.delimiterTail)

    open fun replace(str: String, func: (Int, String) -> String?): String? {
        if (str.isBlank()) return str

        val sb = StringBuilder(str)
        val headDelimiterLen = delimiterHead.length
        val tailDelimiterLen = delimiterTail.length

        var headerIdx = sb.indexOf(delimiterHead)
        var tailIdx = sb.indexOf(delimiterTail, headerIdx)
        var i = 0
        while (headerIdx >= 0 && tailIdx > 0) {
            val key = sb.substring(headerIdx + headDelimiterLen, tailIdx)
            val value = func(i, key)
            if (value != null) {
                sb.replace(headerIdx, tailIdx + tailDelimiterLen, value)
            }
            headerIdx = sb.indexOf(delimiterHead, tailIdx)
            tailIdx = sb.indexOf(delimiterTail, headerIdx)
            i++
        }

        return sb.toString()
    }

    open fun replace(str: String, values: Map<String, String>): String? = replace(str) { _, key -> values[key]  }

    open fun replace(str: String, vararg values: String): String? = replace(str) { i: Int, _: String -> values[i] }
}