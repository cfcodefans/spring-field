package cfcodefans.study.spring_field.commons

import cfcodefans.study.spring_field.commons.Mark.Companion.EMPTY_PREFIX
import cfcodefans.study.spring_field.commons.Mark.Companion.HEAD_PREFIX
import cfcodefans.study.spring_field.commons.Mark.Companion.INFO_PREFIX
import cfcodefans.study.spring_field.commons.Mark.Companion.MARK_PREFIX
import cfcodefans.study.spring_field.commons.Mark.Companion.TAIL_PREFIX
import cfcodefans.study.spring_field.commons.Mark.Companion.indent
import cfcodefans.study.spring_field.commons.Mark.Companion.makeHeader
import cfcodefans.study.spring_field.commons.MiscUtils.invocInfo
import org.apache.commons.lang3.time.DateFormatUtils
import org.slf4j.Logger
import java.util.*

open class Mark(val layer: Int = 1, val index: Int = 1, val info: String = "") {
    open val startAt: Long = System.currentTimeMillis()
    open var time: Long = -1

    companion object {
        const val HEAD_PREFIX: String = "┬───"
        const val INFO_PREFIX: String = "│   "
        const val MARK_PREFIX: String = "├───"
        const val TAIL_PREFIX: String = "└───"
        const val EMPTY_PREFIX: String = "    "
        const val TAB: String = "\t"

        fun indent(info: String,
                   header: String,
                   body: String? = null,
                   tail: String? = null): List<String> = indent(lines = ArrayList(info.lines()),
                                                                header = header,
                                                                body = body,
                                                                tail = tail)

        internal fun indent(lines: List<String>, header: String?, body: String?, tail: String?): List<String> {
            val len: Int = lines.size
            val reList: ArrayList<String> = ArrayList(len)

            if (header?.isNotEmpty() == true) reList += "$header ${lines[0]}"
            if (len == 1) return reList

            if (body?.isNotEmpty() == true) {
                for (i in 1 until (len - 1)) {
                    reList += "$body ${lines[i]}"
                }
            }

            if (tail?.isNotEmpty() == true) {
                reList += "$tail ${lines[len - 1]}"
            }

            return reList
        }

        internal fun makeHeader(lines: List<String>, hasPrevious: Boolean, hasNext: Boolean): Triple<String, String, String> = when {
            lines.isEmpty() -> Triple("", "", "")
            hasPrevious && hasNext -> Triple(MARK_PREFIX, INFO_PREFIX, INFO_PREFIX)
            hasPrevious.not() && hasNext -> Triple(HEAD_PREFIX, INFO_PREFIX, INFO_PREFIX)
            hasPrevious.not() && hasNext.not() -> Triple(TAIL_PREFIX, EMPTY_PREFIX, EMPTY_PREFIX)
            hasPrevious && hasNext.not() -> Triple(TAIL_PREFIX, EMPTY_PREFIX, EMPTY_PREFIX)
            else -> Triple("", "", "")
        }
    }

    internal open fun flash(prefix: String = ""): List<String> {
        if (time < 0) done()
        return indent(info, header = "$prefix (${time} ms)\t")
    }

    open fun result(): String = "(${
        if (time < 0) "${DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(startAt)} - "
        else "$time ms"
    }) $info"

    internal open fun done(): Mark = apply {
        if (time < 0)
            time = System.currentTimeMillis() - startAt
    }

    open fun isDone(): Boolean = time != -1L
}

open class MarkSeq(layer: Int = 1,
                   index: Int = 1,
                   info: String,
                   val marks: LinkedList<Mark> = LinkedList()) : Mark(layer, index, info) {

    open fun mark(info: String): Mark = mark(Mark(
            layer = this.layer,
            index = this.marks.size + 1,
            info = info))

    open fun mark(mark: Mark): Mark {
        marks.peekLast()?.done()
        marks += mark
        return mark
    }

    override fun done(): Mark = apply {
        marks.peekLast()?.done()
        super.done()
    }

    override fun flash(prefix: String): List<String> = super.flash(prefix) + marks.map { it.flash(prefix) }.flatten()
}

open class Trace(var log: Logger? = null) {
    private var stack: LinkedList<MarkSeq> = LinkedList<MarkSeq>()
    var finisheds: LinkedList<MarkSeq> = LinkedList()
    private var table: MutableMap<String, MarkSeq> = hashMapOf()

    fun withLog(log: Logger): Trace = apply { this.log = log }

    fun flash(): List<String> {
        if (stack.isEmpty() && finisheds.isEmpty()) return emptyList()

        var last: MarkSeq? = null
        while (stack.isNotEmpty()) {
            last = stack.peekLast()
            if (!last.isDone()) last.done()
            stack.removeLast()
        }
        if (last != null) finisheds.offerLast(last)

        val reLines = format()
        clean()
        return reLines
    }

    fun format(): List<String> = finisheds.map(Trace::format).flatten()

    fun clean(): Trace = apply {
        stack = LinkedList<MarkSeq>()
        finisheds = LinkedList<MarkSeq>()
        table = hashMapOf()
    }

    companion object {
        private val tracePool: ThreadLocal<Trace> = ThreadLocal.withInitial { Trace() }

        fun current(): Trace = tracePool.get()

        fun ongoing(info: String): Trace = current().apply { mark(info) }

        fun format(markSeq: MarkSeq): List<String> {
            val reList: LinkedList<String> = LinkedList()

            val callers: LinkedList<Pair<MarkSeq, ListIterator<Mark>>> = LinkedList(listOf(markSeq to markSeq.marks.listIterator()))

            do {
                val (currentMarkSeq, iter) = callers.peek()

                val headerStr: String = callers.listIterator(1).asSequence().map { caller ->
                    val (_, callerIter) = caller
                    if (callerIter.hasPrevious() && callerIter.hasNext()) return@map INFO_PREFIX
                    return@map EMPTY_PREFIX
                }.toList().reversed().joinToString("")
                //build header

                // the first mark
                val index: String = callers.map { it.first.index + 1 }.asReversed().joinToString(".")
                if (iter.hasPrevious().not()) {
                    val parentIter: ListIterator<Mark>? = if (callers.size > 1) callers[1].second else null

                    val leadingHeader: String = callers.listIterator(1).asSequence().map { caller ->
                        val (_, callerIter) = caller
                        if (callerIter == parentIter) {
                            if (parentIter.hasPrevious() && parentIter.hasNext()) return@map MARK_PREFIX
                            if (parentIter.hasPrevious().not() && parentIter.hasNext()) return@map HEAD_PREFIX
                            if (parentIter.hasNext().not()) return@map TAIL_PREFIX
                        }
                        if (callerIter.hasPrevious() && callerIter.hasNext()) return@map INFO_PREFIX
                        return@map EMPTY_PREFIX
                    }.toList().reversed().joinToString("")

                    val infoLines: List<String> = currentMarkSeq.result().lines()
                    val (infoHeader, infoMark, infoTail) = makeHeader(infoLines, iter.hasPrevious(), iter.hasNext())
                    val placeHolder: String = " ".repeat(index.length)
                    indent(lines = infoLines,
                           header = "$leadingHeader$infoHeader $index",
                           body = "$headerStr$infoMark $placeHolder",
                           tail = "$headerStr$infoTail $placeHolder")
                        .let { reList.addAll(it) }
                }



                while (iter.hasNext()) {
                    val mark: Mark = iter.next()
                    if (mark is MarkSeq) {
                        callers.push(mark to mark.marks.listIterator())
                        break
                    }

                    val markIndex: String = "${mark.index + 1}"
                    val markIndexPlaceHolder: String = " ".repeat(markIndex.length)

                    val infoLines: List<String> = mark.result().lines()
                    val (infoHeader, infoMark, infoTail) = makeHeader(infoLines, iter.hasPrevious(), iter.hasNext())
                    indent(lines = infoLines,
                           header = "$headerStr$infoHeader $markIndex",
                           body = "$headerStr$infoMark $markIndexPlaceHolder",
                           tail = "$headerStr$infoTail $markIndexPlaceHolder")
                        .let { reList.addAll(it) }
                }

                if (currentMarkSeq === callers.peek().first)
                    callers.pop()
            } while (callers.isNotEmpty())

            return reList
        }


        fun info(log: Logger): (tr: Trace) -> Unit = { it ->
            log.info("\n" + it.flash().joinToString("\n"))
        }

        fun warn(log: Logger): (tr: Trace) -> Unit = { it ->
            log.warn("\n" + it.flash().joinToString("\n"))
        }

        fun debug(log: Logger): (tr: Trace) -> Unit = { it ->
            log.debug("\n" + it.flash().joinToString("\n"))
        }

        fun error(log: Logger): (tr: Trace) -> Unit = { it ->
            log.error("\n" + it.flash().joinToString("\n"))
        }
    }

    private fun pushOut(): MarkSeq? {
        if (stack.isEmpty()) return null

        while (stack.isNotEmpty()) {
            val last: MarkSeq = stack.peekLast()
            if (last.isDone().not()) return last
            stack.removeLast()
        }
        return null
    }

    fun start(info: String): MarkSeq {
        val lastMG: MarkSeq? = pushOut()
        val markSeq: MarkSeq = MarkSeq(layer = stack.size,
                                       index = lastMG?.marks?.size ?: 0,
                                       info = info)
        lastMG?.mark(markSeq)
        stack += markSeq
        return markSeq
    }

    fun mark(info: String): MarkSeq {
        this.log?.info(info)

        val lastMG: MarkSeq? = pushOut()
        if (lastMG == null || lastMG.isDone()) return start(info)
        lastMG.mark(info)
        return lastMG
    }

    fun done(info: String): MarkSeq? {
        if (stack.isEmpty()) {
            start(info)
        }
        val lastMG: MarkSeq? = pushOut()
        if (lastMG != null && !lastMG.isDone()) {
            lastMG.mark(info)
            lastMG.done()
            stack.removeLast()
        }

        if (stack.isEmpty()) finisheds.offerLast(lastMG)
        return lastMG
    }

    fun abort(markGroupInfo: String, info: String): MarkSeq? {
        if (stack.isEmpty()) return null

        val currentMarkSeq: MarkSeq = stack.peekLast()
        if (currentMarkSeq.info == markGroupInfo) {
            done(info)
            return currentMarkSeq
        }

        var reversedIter: MutableListIterator<MarkSeq> = stack.listIterator(stack.size)

        var markSeq: MarkSeq? = null
        while (reversedIter.hasPrevious()) {
            markSeq = reversedIter.previous()
            if (markSeq.info == markGroupInfo) break
        }

        if (markSeq == null) return null

        reversedIter = stack.listIterator(stack.size)
        while (reversedIter.hasPrevious()) {
            val markSeq1 = reversedIter.previous()
            if (markSeq1 === markSeq) break
            markSeq1.done()
            reversedIter.remove()
        }

        done(info)

        return markSeq
    }

    open fun <T> wrap(logOper: (tr: Trace) -> Unit = info(log!!), oper: (tr: Trace) -> T): T {
        val traceTitle: String = invocInfo()
        val startMark: String = this.start(traceTitle).info
        try {
            val re = oper(this)
            this.done("done $traceTitle")
            return re
        } catch (t: Throwable) {
            this.abort(startMark, "catch exception: ${t.message} ${t.stackTrace.joinToString("\t")}".also { log?.error(it) })
            throw t
        } finally {
            logOper(this)
        }
    }

}
