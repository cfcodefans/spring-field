package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger

object AsyncMethodInvocation {

    private val log: Logger = LoggerFactory.getLogger(AsyncMethodInvocation::class.java)

    fun interface IAsyncCallback<T> {
        /**
         * Complete handler which is executed when async task is completed or fails execution.
         *
         * @param value the evaluated value from async task, undefined when execution fails
         * @param ex    empty value if execution succeeds, some exception if executions fails
         */
        fun onComplete(value: T, ex: Exception?)
    }

    /**
     * AsyncResult interface
     * @param <T> parameter returned when getValue is invoked.
     */
    interface IAsyncResult<T> {
        fun isCompleted(): Boolean

        //        @Throws(ExecutionException::class)
        var value: T

        @Throws(InterruptedException::class)
        fun await()
    }

    /**
     * AsyncExecutor interface.
     */
    interface IAsyncExecutor {
        /**
         * Starts processing of an async task. Returns immediately with async result.
         * @param task task to be executed asynchronously
         * @return async result for the task
         */
        fun <T> startProc(task: Callable<T>): IAsyncResult<T?>
        fun <T> startProc(task: Callable<T>, callback: IAsyncCallback<T?>? = null): IAsyncResult<T?>

        @Throws(ExecutionException::class, InterruptedException::class)
        fun <T> endProc(asyncResult: IAsyncResult<T>): T
    }

    const val RUNNING: Int = 0
    const val FAILED: Int = -1
    const val COMPLETED: Int = 1

    open class CompletableResult<T>(private val callback: IAsyncCallback<T?>?) : IAsyncResult<T?> {
        val lock: Object = Object()

        @Volatile
        var state: Int = RUNNING

        override var value: T? = null
            set(value) {
                field = value
                state = COMPLETED
                callback?.onComplete(value, null)
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
            @kotlin.jvm.Throws(ExecutionException::class)
            get(): T? = when (state) {
                COMPLETED -> field
                FAILED -> throw ExecutionException(exception!!)
                else -> throw IllegalStateException("Execution note completed yet")
            }

        var exception: Exception? = null
            set(value) {
                field = value
                state = FAILED
                callback?.onComplete(null, value)
                synchronized(lock) {
                    lock.notifyAll()
                }
            }

        override fun isCompleted(): Boolean = state != RUNNING


        @Throws(InterruptedException::class)
        override fun await() {
            synchronized(lock) {
                while (!isCompleted()) lock.wait()
            }
        }
    }

    open class ThreadAsyncExecutor : IAsyncExecutor {
        /**
         * Index of thread naming
         */
        val idx: AtomicInteger = AtomicInteger(0)

        override fun <T> startProc(task: Callable<T>): IAsyncResult<T?> = startProc(task, null)

        override fun <T> startProc(task: Callable<T>, callback: IAsyncCallback<T?>?): IAsyncResult<T?> {
            val result: CompletableResult<T?> = CompletableResult(callback)
            Thread() {
                try {
                    result.value = task.call()
                } catch (ex: Exception) {
                    result.exception = ex
                }
            }.apply { name = "executor-${idx.incrementAndGet()}" }
                .start()
            return result
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        override fun <T> endProc(asyncResult: IAsyncResult<T>): T {
            if (asyncResult.isCompleted().not()) asyncResult.await()
            return asyncResult.value
        }
    }

    fun <T> lazyVal(value: T, delayMillis: Long): Callable<T> = Callable<T> {
        Thread.sleep(delayMillis)
        log.info("Space rocket <$value> launched successfully")
        value
    }

    /**
     * Creates a simple callback that logs the complete status of that async result.
     */
    private fun <T> callback(name: String): IAsyncCallback<T> {
        return IAsyncCallback<T> { v: T, ex: Exception? ->
            if (ex != null)
                log.error("name failed: ${ex ?: ""}")
            else
                log.info("$name <$v>")
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun main(args: Array<String>) {
        //construct a new executor that will run async tasks
        val executor: IAsyncExecutor = ThreadAsyncExecutor()

        // start few async tasks with varying processing times, two last with callback
        val asyncRe1: IAsyncResult<Int?> = executor.startProc(lazyVal(10, 500L))
        val asyncRe2: IAsyncResult<String?> = executor.startProc(lazyVal("test", 300L))
        val asyncRe3: IAsyncResult<Long?> = executor.startProc(lazyVal(50L, 700L))
        val asyncRe4: IAsyncResult<Int?> = executor.startProc(lazyVal(20, 400), callback("Deploying lunar rover"))
        val asyncRe5: IAsyncResult<String?> = executor.startProc(lazyVal("callback", 600), callback("Deploying lunar rover"))

        // emulate processing in the current thread while async tasks are running in their own threads
        Thread.sleep(350) // Oh boy, we are working hard here
        log.info("Missing command is sipping coffee.")

        //wait for completion of the tasks
        val re1: Int? = executor.endProc(asyncRe1)
        val re2: String? = executor.endProc(asyncRe2)
        val re3: Long? = executor.endProc(asyncRe3)
        asyncRe4.await()
        asyncRe5.await()

        // log the results of the tasks, callbacks log immediately when complete
        log.info("Space rocket <$re1> launched successfully")
        log.info("Space rocket <$re2> launched successfully")
        log.info("Space rocket <$re3> launched successfully")

    }
}