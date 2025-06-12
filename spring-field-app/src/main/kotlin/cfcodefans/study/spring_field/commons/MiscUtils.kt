package cfcodefans.study.spring_field.commons

import jakarta.xml.bind.JAXB
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object MiscUtils {

    fun invocInfo(): String {
        val ste = Thread.currentThread().stackTrace
        val i = 2
        return "${StringUtils.substringAfterLast(ste[i].className, ".")}.${ste[2].methodName}"
    }


    val keyAndSha512HmacUtils: ConcurrentHashMap<String, HmacUtils> = ConcurrentHashMap()

    fun getHmacUtilsByKey(key: String): HmacUtils = keyAndSha512HmacUtils.computeIfAbsent(key) { HmacUtils(HmacAlgorithms.HMAC_SHA_512.getName(), it) }

    fun toXML(obj: Any): String = ByteArrayOutputStream()
        .also { JAXB.marshal(obj, it) }
        .toByteArray()
        .toString(Charset.defaultCharset())


    fun <T> fromXML(xmlStr: String?, clz: Class<T>): T? = if (xmlStr.isNullOrBlank()) null else JAXB.unmarshal(StringReader(xmlStr), clz)

    fun toXML1(bean: Any): String? {
        val sw = StringWriter()
        try {
            val jc = JAXBContext.newInstance(bean.javaClass)
            val m = jc.createMarshaller()
            m.setProperty(Marshaller.JAXB_FRAGMENT, true)
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            m.marshal(bean, sw)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sw.toString()
    }

    inline fun <reified T> fromXML1(xmlStr: String?, clz: Class<T>): T? {
        if (xmlStr.isNullOrBlank()) return null
        try {
            val jc = JAXBContext.newInstance(clz)
            val um = jc.createUnmarshaller()
            return um.unmarshal(StringReader(xmlStr)) as T?
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun toSingleMap(strToArr: Map<String, Array<String>?>?): Map<String, String?> {
        return (strToArr ?: emptyMap()).entries.associate { en -> en.key to en.value?.first() }
    }

    fun sleep(timeMs: Long): Throwable? = runCatching { Thread.sleep(timeMs) }.exceptionOrNull()

    @Throws(InterruptedException::class)
    fun interrupted(msg: String): Boolean = if (Thread.interrupted())
        throw InterruptedException(msg)
    else
        false
}

inline infix fun <T, R> (() -> T).andThen(crossinline block: T.() -> R): () -> R = { block(this()) }

inline infix fun <T> (() -> T).andThrow(crossinline throwable: T.() -> Throwable): () -> Nothing = andThen {
    throw throwable()
}

infix fun <T> (() -> T).andThrow(throwable: Throwable): () -> Nothing = andThrow { throwable }

fun String?.requireNotEmpty(throwable: Throwable = IllegalArgumentException("can't be empty"),
                            elseBlock: () -> Unit = {}): String = if (!isNullOrEmpty()) this else {
    run(elseBlock.andThrow(throwable))
}

fun String?.requireNotEmpty(message: String = "can't be empty", elseBlock: () -> Unit = {}): String =
    requireNotEmpty(IllegalArgumentException(message), elseBlock)

fun String?.requireNotEmpty(): String = requireNotEmpty("can't be empty")

fun String?.blankOr(defaultStr: String?): String? = if (this.isNullOrBlank()) defaultStr else this

/**
 * refers to https://medium.com/@jerzy.chalupski/emulating-self-types-in-kotlin-d64fe8ea2e62
 */
interface ITypeHolder<T : ITypeHolder<T>> {
    @Suppress("UNCHECKED_CAST")
    fun self(): T = this as T
    fun applySelf(block: T.() -> Unit): T = self().apply { block() }
}

open class ExByteArrayInputStream : ByteArrayInputStream {
    fun getBuf(): ByteArray = super.buf

    constructor(buf: ByteArray) : super(buf)
    constructor(bufSize: Int) : super(ByteArray(bufSize))
}

open class ExByteArrayOutputStream : ByteArrayOutputStream {
    fun getBuf(): ByteArray = super.buf
    fun setBuf(ba: ByteArray) {
        this.reset()
        this.buf = ba
    }

    constructor() : super()
    constructor(capacity: Int) : super(capacity)
    constructor(buf: ByteArray) : super() {
        super.buf = buf
    }

    override fun flush() {}
    override fun toByteArray(): ByteArray = super.buf
}

open class Finalizable<C : AutoCloseable>(val cl: C) : AutoCloseable {
    override fun close() = cl.close()

//    @Throws(Throwable::class)
//    protected fun finalize() {
//        cl.close()
//    }
}

/**
 * caveat, not-thread-safe
 */
open class CloseableMgr(val resources: MutableSet<AutoCloseable> = LinkedHashSet<AutoCloseable>(),
                        var onError: (AutoCloseable, Throwable) -> Unit = { c, e -> logError(c, e) }) : AutoCloseable {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CloseableMgr::class.java)
        fun logError(res: AutoCloseable, e: Throwable): Unit {
            log.error("failed to call close method on: $res", e)
        }

        inline fun <reified T : AutoCloseable> T.reg(ctx: CloseableMgr): T = apply { ctx.resources.add(this) }
    }

    override fun close() {
        for (res in resources.reversed()) {
            try {
                res.close()
            } catch (e: Throwable) {
                onError(res, e)
            }
        }
        resources.clear()
    }
}

/**
 * An enhanced, safer AutoCloseable wrapper for a ThreadLocal instance.
 *
 * Key features:
 * - Ensures ThreadLocal.remove() is called via the AutoCloseable contract.
 * - Makes the close() operation idempotent (safe to call multiple times).
 * - Includes basic null check for the provided ThreadLocal.
 *
 * Usage with try-with-resources (Java) or 'use' extension function (Kotlin)
 * guarantees cleanup for a specific scope.
 *
 * @param T The type of the value stored in the ThreadLocal.
 * @property threadLocal The underlying ThreadLocal instance being managed. Must not be null.
 * @throws NullPointerException if threadLocal is null.
 */
open class AutoCloseableThreadLocal<T>(private val threadLocal: ThreadLocal<T>) : AutoCloseable {

    // Flag to ensure remove() logic runs only once per wrapper instance.
    // AtomicBoolean is robust even if the wrapper instance were somehow shared
    // and closed concurrently (though unlikely in typical 'use' patterns).
    private val closed = AtomicBoolean(false)

    init {
        // Eagerly check for null input during construction
        // (Though Kotlin's type system helps, this adds robustness if called from Java)
        // In pure Kotlin with non-nullable type ThreadLocal<T>, this might be redundant
        // but doesn't hurt. If T could be nullable (ThreadLocal<T?>), it's still needed.
        // requireNotNull(threadLocal) { "ThreadLocal instance cannot be null." } // Alternative way
        if (threadLocal == null) throw NullPointerException("ThreadLocal instance cannot be null.")
    }

    /**
     * Calls remove() on the underlying ThreadLocal instance for the current thread,
     * ensuring this operation happens only once per instance of this wrapper.
     *
     * This method is idempotent (safe to call multiple times). It's automatically
     * invoked at the end of a try-with-resources block (Java) or a 'use' block (Kotlin).
     */
    override fun close() {
        // Use compareAndSet for an atomic check-and-set operation.
        // Ensures that the block inside runs at most once.
        if (closed.compareAndSet(false, true)) {
            val threadId = Thread.currentThread().id
            var valueBefore: Any? = "[Value could not be retrieved before remove]" // Default message

            try {
                // Log the value *before* removing, requires getting it first.
                valueBefore = threadLocal.get()
                // The core action: remove the value for the current thread.
                threadLocal.remove()
                // Log success
                log.info("SafeAutoCloseableThreadLocal@${System.identityHashCode(this)}: Closed -> Removed TL value '$valueBefore' for TL@${System.identityHashCode(threadLocal)} on thread $threadId")

            } catch (e: Throwable) {
                // Log unexpected errors during get() or remove()
                // It's highly unlikely for ThreadLocal.remove() to throw exceptions
                // unless maybe security manager issues, but catching Throwable is safest.
                log.error("""SafeAutoCloseableThreadLocal@${System.identityHashCode(this)}: 
                    |Unexpected error during close/remove for TL@${System.identityHashCode(threadLocal)} 
                    |on thread $threadId. 
                    |Value before remove attempt: '$valueBefore'""".trimMargin(), e)
                // Depending on policy, you might want to re-throw, but typically
                // exceptions during close() are suppressed in try-with-resources.
            }
        }
        // If already closed (compareAndSet returned false), do nothing.
    }

    // --- Optional: Companion object for factory methods ---
    companion object {
        val log: Logger = LoggerFactory.getLogger(AutoCloseableThreadLocal::class.java)

        /**
         * Factory method for convenience and potentially adding checks.
         */
        @JvmStatic
        fun <T> manage(threadLocal: ThreadLocal<T>): AutoCloseableThreadLocal<T> {
            // The null check is now handled in the constructor called below
            return AutoCloseableThreadLocal(threadLocal)
        }
    }
}