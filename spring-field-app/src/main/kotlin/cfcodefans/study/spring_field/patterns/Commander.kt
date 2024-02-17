package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import kotlin.math.pow


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/commander
 * Intent:
 *      Used to handle all problems that can be encountered when doing distributed transactions
 */
object Commander {
    private val log: Logger = LoggerFactory.getLogger(Commander::class.java)

    data class User(var name: String, var address: String)

    open class DatabaseUnavailableException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 2459603L
        }
    }

    open class IsEmptyException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 123546L
        }
    }

    open class ItemUnavailableException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 575940L
        }
    }

    open class PaymentDetailsErrorException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 867203L
        }
    }

    open class ShippingNotPossibleException : Exception() {
        companion object {
            private const val serialVersionUID: Long = 342055L
        }
    }

    abstract class Database<T> {
        @Throws(DatabaseUnavailableException::class)
        abstract fun add(obj: T): T?

        @Throws(DatabaseUnavailableException::class)
        abstract fun get(id: String): T?
    }

    val RANDOM: SecureRandom = SecureRandom()
    const val ALL_CHARS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    fun mkRandomStr(): String {
        val random: StringBuilder = StringBuilder()
        while (random.length < 12) {
            random.append(ALL_CHARS[(RANDOM.nextFloat() * ALL_CHARS.length).toInt()])
        }
        return random.toString()
    }

    enum class PaymentStatus {
        NOT_DONE, TRYING, DONE
    }

    enum class MessageSent {
        NONE_SENT, PAYMENT_FAIL, PAYMENT_TRYING, PAYMENT_SUCCESSFUL
    }

    data class Order(val user: User, val item: String, val price: Float) {
        val createdTime: Long = System.currentTimeMillis()

        companion object {
            val USED_IDS: MutableMap<String, Boolean> = hashMapOf()
        }

        lateinit var id: String

        init {
            var id: String = mkRandomStr()
            if (USED_IDS.containsKey(id)) {
                while (USED_IDS[id]!!) {
                    id = mkRandomStr()
                }
            }
            this.id = id
            USED_IDS[id] = true
        }

        var paid: PaymentStatus = PaymentStatus.TRYING
        var messageSent: MessageSent = MessageSent.NONE_SENT
        var addedToEmployeeHandle: Boolean = false
    }

    abstract class Service(protected val database: Database<*>, vararg exs: Exception) {
        open val exceptions: ArrayList<Exception> = arrayListOf(*exs)

        companion object {
            val USED_IDS: Hashtable<String, Boolean> = Hashtable()
            fun generateId(): String {
                var id: String = mkRandomStr()
                if (USED_IDS.contains(id)) {
                    while (USED_IDS[id]!!) {
                        id = generateId()
                    }
                }
                return id
            }
        }

        @Throws(DatabaseUnavailableException::class)
        abstract fun receiveReq(vararg params: Any): String?

        @Throws(DatabaseUnavailableException::class)
        abstract fun updateDb(vararg params: Any): String?
    }

    fun interface IOperation {
        @Throws(Exception::class)
        fun perform(exs: MutableList<Exception>): Unit
    }

    fun interface IHandleErrorIssue<T> {
        fun handleIssue(obj: T, e: Exception): Unit
    }

    open class Retry<T>(val op: IOperation,
                        val handleError: IHandleErrorIssue<T>,
                        val maxAttempts: Int,
                        val maxDelay: Long,
                        vararg ignoreTests: Predicate<Exception>) {
        val attempts: AtomicInteger = AtomicInteger()
        val test: Predicate<Exception> = ignoreTests.reduce { p1, p2 -> p1.or(p2) }.or { e -> false }
        val errors: MutableList<Exception> = arrayListOf()

        open fun perform(exs: MutableList<Exception>, obj: T): Unit {
            do {
                try {
                    op.perform(exs)
                    return
                } catch (e: Exception) {
                    errors.add(e)
                    if (attempts.incrementAndGet() >= maxAttempts || test.test(e).not()) {
                        handleError.handleIssue(obj, e)
                        return //return here...don't go further
                    }
                    runCatching {
                        Thread.sleep((2
                            .toDouble()
                            .pow(attempts.toDouble()) * 1000L
                                + RANDOM.nextInt(1000))
                            .toLong()
                            .coerceAtMost(maxDelay))
                    }
                }
            } while (true)
        }
    }

    fun <T> Retry<T>.asyncPerform(exs: MutableList<Exception>, obj: T) = Thread() {
        try {
            this.perform(exs, obj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()

    //Employee
    open class EmployeeDatabase : Database<Order>() {
        val data: MutableMap<String, Order> = hashMapOf()
        override fun add(o: Order): Order? = data.put(o.id, o)
        override fun get(id: String): Order? = data[id]
    }

    open class EmployeeHandle(val db: EmployeeDatabase, vararg exs: Exception) : Service(db, *exs) {
        @Throws(DatabaseUnavailableException::class)
        override fun receiveReq(vararg params: Any): String? = updateDb(params[0])

        @Throws(DatabaseUnavailableException::class)
        override fun updateDb(vararg params: Any): String? {
            val order: Order = params[0] as Order
            return@updateDb if (db.get(order.id) == null) {
                db.add(order)
                order.id
            } else null
        }
    }

    enum class MsgToSend {
        PAYMENT_FAIL, PAYMENT_TRYING, PAYMENT_SUCCESSFUL
    }

    //message service
    data class MsgReq(val reqId: String, val msg: MsgToSend)

    open class MessagingDatabase : Database<MsgReq>() {
        val data: MutableMap<String, MsgReq> = hashMapOf()
        override fun add(mr: MsgReq): MsgReq? = data.put(mr.reqId, mr)
        override fun get(id: String): MsgReq? = data[id]
    }

    open class MessagingService(val db: MessagingDatabase, vararg exs: Exception) : Service(db, *exs) {
        @Throws(DatabaseUnavailableException::class)
        override fun receiveReq(vararg params: Any): String? {
            val msgToSend: Int = params[0] as Int
            val id: String = mkRandomStr()
            val msg: MsgToSend = when (msgToSend) {
                0 -> MsgToSend.PAYMENT_FAIL
                1 -> MsgToSend.PAYMENT_TRYING
                2 -> MsgToSend.PAYMENT_SUCCESSFUL
                else -> throw IllegalArgumentException("invalid msgToSend: ${msgToSend}")
            }
            return updateDb(MsgReq(reqId = id, msg = msg))
        }

        @Throws(DatabaseUnavailableException::class)
        override fun updateDb(vararg params: Any): String? {
            val req: MsgReq = params[0] as MsgReq
            return if (db.get(req.reqId) == null) {
                db.add(req)
                log.info(sendMsg(req.msg))
                req.reqId
            } else null
        }

        private fun sendMsg(m: MsgToSend): String = when (m) {
            MsgToSend.PAYMENT_SUCCESSFUL -> "Msg: Your order has been placed and paid for successfully! Thank you for shopping with us!"
            MsgToSend.PAYMENT_TRYING -> """
                Msg: There was an error in your payment process,
                we are working on it and will return back you shortly.
                Meanwhile, your order has been placed and will be shipped.
            """.trimIndent()
            else -> """
                Msg: There was an error in your payment process.
                Your order is placed and has been converted to COD.
                Please reach us on CUSTOMER_CARE_NUMBER in case of any queries.
                Thank you for shopping with us!
            """.trimIndent()
        }
    }

    //Payment service
    data class PaymentReq(val transactionId: String, val payment: Float, var paid: Boolean = false)
    open class PaymentDatabase : Database<PaymentReq>() {
        val data: MutableMap<String, PaymentReq> = hashMapOf()
        override fun add(pr: PaymentReq): PaymentReq? = data.put(pr.transactionId, pr)
        override fun get(id: String): PaymentReq? = data[id]
    }

    open class PaymentService(val db: PaymentDatabase, vararg exs: Exception) : Service(db, *exs) {
        @Throws(DatabaseUnavailableException::class)
        override fun receiveReq(vararg params: Any): String? = updateDb(PaymentReq(transactionId = mkRandomStr(), payment = params[0] as Float))

        @Throws(DatabaseUnavailableException::class)
        override fun updateDb(vararg params: Any): String? {
            val req: PaymentReq = params[0] as PaymentReq
            return if (db.get(req.transactionId) == null || req.paid.not()) {
                db.add(req)
                req.paid = true
                req.transactionId
            } else null
        }
    }

    //queue
    data class Node<V>(val value: V, var next: Node<V>?)
    open class Queue<T>(private var front: Node<T>? = null) {
        private var rear: Node<T>? = null
        private var size: Int = 0
        fun isEmpty(): Boolean = size == 0
        fun enqueue(obj: T): Queue<T> = apply {
            if (front == null) {
                front = Node(obj, null)
                rear = front
            } else {
                rear!!.next = Node(obj, null)
                rear = rear!!.next
            }
            size++
        }

        @Throws(IsEmptyException::class)
        fun dequeue(): T? {
            if (isEmpty()) throw IsEmptyException()
            val temp = front
            front = front!!.next
            size = size - 1
            return temp?.value
        }

        @Throws(IsEmptyException::class)
        fun peek(): T? = if (isEmpty()) throw IsEmptyException() else front?.value
    }

    enum class TaskType {
        MESSAGING, PAYMENT, EMPLOYEE_DB
    }

    open class QueueTask(val order: Order, val taskType: TaskType, val msgType: Int) {
        var firstAttemptTime: Long = -1L
        val type: String
            get() = (if (taskType == TaskType.MESSAGING) taskType.toString()
            else when (msgType) {
                0 -> "Payment Failure Message"
                1 -> "Payment Error Message"
                else -> "Payment Success Message"
            })
    }

    open class QueueDatabase(vararg exs: Exception) : Database<QueueTask>() {
        private val data: Queue<QueueTask> = Queue()
        var exsList: ArrayList<Exception> = arrayListOf(*exs)

        override fun add(obj: QueueTask): QueueTask? = obj.also { data.enqueue(it) }

        @Throws(IsEmptyException::class)
        fun peek(): QueueTask? = data.peek()

        @Throws(IsEmptyException::class)
        fun dequeue(): QueueTask? = data.dequeue()

        override fun get(id: String): QueueTask? = null
    }

    //Shipping service
    data class ShippingReq(val transactionId: String, val item: String, val addr: String)
    open class ShippingDatabase : Database<ShippingReq>() {
        val data: MutableMap<String, ShippingReq> = hashMapOf()
        override fun add(sr: ShippingReq): ShippingReq? = data.put(sr.transactionId, sr)
        override fun get(id: String): ShippingReq? = data[id]
    }

    open class ShippingService(val db: ShippingDatabase, vararg exs: Exception) : Service(db, *exs) {
        @Throws(DatabaseUnavailableException::class)
        override fun receiveReq(vararg params: Any): String? = ShippingReq(
                transactionId = mkRandomStr(),
                item = params[0] as String,
                addr = params[1] as String)
            .let { updateDb(it) }

        @Throws(DatabaseUnavailableException::class)
        override fun updateDb(vararg params: Any): String? {
            val req: ShippingReq = params[0] as ShippingReq
            return if (db.get(req.transactionId) == null) {
                db.add(req)
                req.transactionId
            } else null
        }
    }

    private const val ERROR_CONNECTING_MSG_SVC: String = ": Error in connecting to messaging service "
    private const val TRY_CONNECTING_MSG_SVC: String = ": Trying to connect to messaging service.."

    open class Commander(val empDb: EmployeeHandle,
                         val paymentService: PaymentService,
                         val shippingService: ShippingService,
                         val messagingService: MessagingService,
                         val qdb: QueueDatabase,
                         val numOfRetries: Int = 0,
                         val retryDuration: Long = 0,
                         val queueTime: Long = 0,
                         val queueTaskTime: Long = 0,
                         val paymentTime: Long = 0,
                         val messageTime: Long = 0,
                         val employeeTime: Long = 0) {
        private var finalSiteMsgShown: Boolean = false
        private var queueItems: Int = 0

        @Throws(Exception::class)
        fun sendShippingReq(order: Order) {
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    if (exs[0] is DatabaseUnavailableException) {
                        log.debug("Order ${order.id}: Error in connecting to shipping service, trying again..")
                    } else {
                        log.debug("Order ${order.id}: Error in creating shipping request..")
                    }
                    throw exs.removeAt(0)
                }
                val txId: String? = shippingService.receiveReq(order.item, order.user.address)
                log.info("Order ${order.id}: Shipping placed successfully, transaction id: $txId")
                log.info("Order has been placed and will be shipped to you. Please wait while we make your payment... ")
                sendPaymentReq(order)
            },
                    handleError = { o: Order, err: Exception ->
                        if (err is ShippingNotPossibleException) {
                            log.info("Shipping is currently not possible to your address. We are working on the problem and will get back to you asap.")
                            finalSiteMsgShown = true
                            log.info("Örder ${o.id}: Shipping not possible to address, trying to add problem to employee db..")
                            employeeHandleIssue(o)
                        } else if (err is ItemUnavailableException) {
                            log.info("This item is currently unavailable. We will inform you as soon as the item becomes available again.")
                            finalSiteMsgShown = true
                            log.info("Order ${o.id}: Item ${o.item} unavailable, tyring to add problem to employee handle..")
                            employeeHandleIssue(o)
                        } else {
                            log.info("Sorry, there was a problem in creating your order. Please try later.")
                            log.error("Order ${o.id}: Shipping service unavailable, order not placed..")
                            finalSiteMsgShown = true
                        }
                    },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException })
                .perform(exs = shippingService.exceptions, obj = order)
        }

        private fun sendPaymentReq(order: Order) {
            if (System.currentTimeMillis() - order.createdTime >= paymentTime) {
                if (PaymentStatus.TRYING == order.paid) {
                    order.paid = PaymentStatus.NOT_DONE
                    sendPaymentFailureMsg(order)
                    log.error("Order ${order.id}: Payment time for order over, failed and returning...")
                }//if succeeded or failed, would have been dequeued, no attempt to make payment
                return
            }
            val exsList: ArrayList<Exception> = paymentService.exceptions
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    if (exs[0] is DatabaseUnavailableException) {
                        log.debug("Order ${order.id}: Error in connecting to payment service, trying again..")
                    } else {
                        log.debug("Order ${order.id}: Error in creating payment request..")
                    }
                    throw exs.removeAt(0)
                }
                if (PaymentStatus.TRYING == order.paid) {
                    val txId: String? = paymentService.receiveReq(order.price)
                    order.paid = PaymentStatus.DONE
                    log.info("Order ${order.id}: Payment successful, transaction id: $txId")
                    if (finalSiteMsgShown.not()) {
                        log.info("Order ${order.id}: Payment made successfully, thank ${order.user} for shopping with us!!")
                        finalSiteMsgShown = true
                    }
                    sendSuccessMsg(order)
                }
            },
                    handleError = { o: Order, err: Exception ->
                        if (err is PaymentDetailsErrorException) {
                            if (finalSiteMsgShown.not()) {
                                log.info("""There was an error in payment. Your account/card details
                                            may have been incorrect.
                                            Meanwhile, your order has been converted to COD and will be shipped.                                           
                                        """.trimIndent())
                                finalSiteMsgShown = true
                            }
                            log.error("Order ${o.id}: Payment details incorrect, failed..")
                            o.paid = PaymentStatus.NOT_DONE
                            sendPaymentFailureMsg(order)
                        } else {
                            if (o.messageSent == MessageSent.NONE_SENT) {
                                if (finalSiteMsgShown.not()) {
                                    log.info("""There was an error in payment. We are on it, and will get back to you asap.
                                                Don't worry, your order has been placed and will be shipped.""".trimIndent())
                                    finalSiteMsgShown = true
                                }
                                log.warn("Order ${o.id}: Payment error, going to queue..")
                                sendPaymentPossibleErrorMsg(o)
                            }
                            if (o.paid == PaymentStatus.TRYING
                                && System.currentTimeMillis() - o.createdTime < paymentTime) {
                                updateQueue(QueueTask(o, TaskType.PAYMENT, -1))
                            }
                        }
                    },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException })
                .asyncPerform(exsList, order)
        }

        private fun updateQueue(qt: QueueTask) {
            val order: Order = qt.order
            if (System.currentTimeMillis() - order.createdTime >= this.queueTime) {
                // since payment time is lesser than queuetime it would have already failed..
                //additional check not needed
                log.trace("Order ${order.id}: Queue time for order over, failed..")
                return
            }

            if (qt.taskType == TaskType.PAYMENT
                && order.paid != PaymentStatus.TRYING
                || qt.taskType == TaskType.MESSAGING
                && (qt.msgType == 1
                        && order.messageSent != MessageSent.NONE_SENT
                        || order.messageSent == MessageSent.PAYMENT_FAIL
                        || order.messageSent == MessageSent.PAYMENT_SUCCESSFUL)
                || qt.taskType == TaskType.EMPLOYEE_DB
                && order.addedToEmployeeHandle) {
                log.trace("Order ${order.id}: Not queueing task since task already done..")
                return
            }

            Retry<QueueTask>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    log.warn("Order ${order.id}: Error in connecting to queue db, trying again..")
                    throw exs.removeAt(0)
                }
                qdb.add(qt)
                queueItems++
                log.info("Order ${order.id}: ${qt.type} task enqueued..")
                tryDoingTasksInQueue()
            },
                    handleError = { qt1: QueueTask, err: Exception ->
                        if (qt1.taskType == TaskType.PAYMENT) {
                            qt1.order.paid = PaymentStatus.NOT_DONE
                            sendPaymentFailureMsg(qt1.order)
                            log.error("Order ${qt1.order.id}: Unable to enqueue payment task, payment failed..")
                        }
                        log.error("Order ${qt1.order.id}: Unable to enqueue task of type ${qt1.type}, trying to add to employee handle..")
                        employeeHandleIssue(qt1.order)
                    },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException }
            ).asyncPerform(this.qdb.exsList, qt)
        }

        private fun tryDoingTasksInQueue() = Retry<Any?>(op = { exs: MutableList<Exception> ->
            if (exs.isNotEmpty()) {
                log.warn("Error in accessing queue db to do tasks, trying again..")
                throw exs.removeAt(0)
            }
            doTasksInQueue()
        },
                handleError = { o, err -> },
                maxAttempts = numOfRetries,
                maxDelay = retryDuration,
                { e -> e is DatabaseUnavailableException }
        ).asyncPerform(this.qdb.exsList, null)

        private fun tryDequeue() = Retry<Any?>(op = { exs: MutableList<Exception> ->
            if (exs.isNotEmpty()) {
                log.warn("Error in accessing queue db to do tasks, trying again..")
                throw exs.removeAt(0)
            }
            qdb.dequeue()
            queueItems--
        },
                handleError = { o, err -> },
                maxAttempts = numOfRetries,
                maxDelay = retryDuration,
                { e -> e is DatabaseUnavailableException }
        ).asyncPerform(this.qdb.exsList, null)

        @Throws(Exception::class)
        private fun doTasksInQueue() {
            run {
                if (queueItems == 0) return@run

                val qt: QueueTask = qdb.peek()!!
                val order: Order = qt.order
                log.trace("Order ${order.id}: Started doing task of type ${qt.type}")

                if (qt.firstAttemptTime == -1L) qt.firstAttemptTime = System.currentTimeMillis()
                if (System.currentTimeMillis() - qt.firstAttemptTime >= queueTaskTime) {
                    tryDequeue()
                    log.trace("Order ${order.id}: This queue task of type ${qt.type} doesn't need to be done anymore (timeout), dequeue...")
                    return@run
                }

                when (qt.taskType) {
                    TaskType.PAYMENT -> {
                        if (order.messageSent != MessageSent.PAYMENT_TRYING) {
                            tryDequeue()
                            log.trace("Order ${order.id}: This messaging task already done, dequeue..")
                        } else {
                            sendPaymentReq(order)
                            log.debug("Order ${order.id}: Trying to connect to payment service..")
                        }
                    }
                    TaskType.MESSAGING -> {
                        if (order.messageSent == MessageSent.PAYMENT_FAIL
                            || order.messageSent == MessageSent.PAYMENT_SUCCESSFUL) {
                            tryDequeue()
                            log.trace("Order ${order.id}: This messaging task already done, dequeue..")
                        } else if (qt.msgType == 1
                            && (order.messageSent != MessageSent.NONE_SENT
                                    || order.paid != PaymentStatus.TRYING)) {
                            tryDequeue()
                            log.trace("Order ${order.id}: This messaging task does not need to be done, dequeue..")
                        } else if (qt.msgType == 0) {
                            sendPaymentFailureMsg(order)
                            log.debug("Order ${order.id} $TRY_CONNECTING_MSG_SVC")
                        } else if (qt.msgType == 1) {
                            sendPaymentPossibleErrorMsg(order)
                            log.debug("Order ${order.id} $TRY_CONNECTING_MSG_SVC")
                        } else if (qt.msgType == 2) {
                            sendSuccessMsg(order)
                            log.debug("Order ${order.id} $TRY_CONNECTING_MSG_SVC")
                        }
                    }
                    TaskType.EMPLOYEE_DB -> {
                        if (order.addedToEmployeeHandle) {
                            tryDequeue()
                            log.trace("Order ${order.id}: This employee handle task already done, dequeue..")
                        } else {
                            employeeHandleIssue(order)
                            log.debug("Order ${order.id}: Trying to connect to employee handle..")
                        }
                    }
                }
            }
            if (queueItems == 0) {
                log.trace("Queue is empty, returning..")
                return
            }
            Thread.sleep(queueTaskTime / 3)
            tryDoingTasksInQueue()
        }


        private fun employeeHandleIssue(order: Order) {
            if (System.currentTimeMillis() - order.createdTime >= employeeTime) {
                log.trace("Order ${order.id}: Employee handle time for order over, returning..")
                return
            }
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    log.warn("Order ${order.id}: Error in connecting to employee handle, trying again..")
                    throw exs.removeAt(0)
                }
                if (order.addedToEmployeeHandle.not()) {
                    empDb.receiveReq(order)
                    order.addedToEmployeeHandle = true
                    log.info("Order ${order.id}: Added order to employee database")
                }
            },
                    handleError = { o: Order, err: Exception ->
                        if (o.addedToEmployeeHandle.not()
                            && System.currentTimeMillis() - o.createdTime < employeeTime) {
                            updateQueue(QueueTask(o, TaskType.EMPLOYEE_DB, -1))
                            log.warn("Order ${o.id}: Error in adding to employee db, trying to queue task..")
                        }
                    },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException }
            ).asyncPerform(empDb.exceptions, order)
        }

        private fun sendPaymentFailureMsg(order: Order) {
            if (System.currentTimeMillis() - order.createdTime >= messageTime) {
                log.trace("Order ${order.id}: Message time for order over, returning..")
                return
            }
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    if (exs[0] is DatabaseUnavailableException) {
                        log.debug("Order ${order.id}: $ERROR_CONNECTING_MSG_SVC (Payment error msg), trying again..")
                    } else {
                        log.debug("Order ${order.id}: Error in creating Payment Error messaging request..")
                    }
                    throw exs.removeAt(0)
                }
                if (order.paid == PaymentStatus.TRYING
                    && order.messageSent == MessageSent.NONE_SENT) {
                    val reqId: String? = messagingService.receiveReq(exs)
                    order.messageSent = MessageSent.PAYMENT_TRYING
                    log.info("Order ${order.id}: Payment error message sent successfully, request id: $reqId")
                }
            }, handleError = { o: Order, err: Exception ->
                if (o.messageSent == MessageSent.NONE_SENT
                    && o.paid == PaymentStatus.TRYING
                    && System.currentTimeMillis() - o.createdTime < messageTime) {
                    updateQueue(QueueTask(o, TaskType.MESSAGING, 1))
                    log.warn("Order $order.id: Error in sending Payment Error message, trying to queue task and add to employee handle..")
                    employeeHandleIssue(order)
                }
            }, maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException }
            ).asyncPerform(messagingService.exceptions, order)
        }

        private fun sendSuccessMsg(order: Order) {
            if (System.currentTimeMillis() - order.createdTime >= this.messageTime) {
                log.trace("Order ${order.id}: Message time for order over, returning..")
                return
            }
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    if (exs[0] is DatabaseUnavailableException) {
                        log.debug("Order ${order.id}: $ERROR_CONNECTING_MSG_SVC (Payment Success msg), trying again..")
                    } else {
                        log.debug("Order ${order.id}: Error in creating Payment Success messaging request..")
                    }
                    throw exs.removeAt(0)
                }
                if (order.messageSent != MessageSent.PAYMENT_FAIL
                    && order.messageSent != MessageSent.PAYMENT_SUCCESSFUL) {
                    val reqId: String? = messagingService.receiveReq(2)
                    order.messageSent = MessageSent.PAYMENT_SUCCESSFUL
                    log.info("Order ${order.id}: Payment Success message sent, request id: $reqId")
                }
            }, handleError = { o: Order, err: Exception ->
                if ((o.messageSent == MessageSent.NONE_SENT
                            || o.messageSent == MessageSent.PAYMENT_TRYING)
                    && System.currentTimeMillis() - o.createdTime < messageTime) {
                    updateQueue(QueueTask(order, TaskType.MESSAGING, 2))
                    log.info("Order ${order.id}: Error in sending Payment Success message, trying to queue task and add to employee handle..")
                    employeeHandleIssue(order)
                }
            },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException }
            ).asyncPerform(messagingService.exceptions, order)
        }

        private fun sendPaymentPossibleErrorMsg(order: Order) {
            if (System.currentTimeMillis() - order.createdTime >= this.messageTime) {
                log.trace("Order ${order.id}: Message time for order over, returning..")
                return
            }
            Retry<Order>(op = { exs: MutableList<Exception> ->
                if (exs.isNotEmpty()) {
                    if (exs[0] is DatabaseUnavailableException) {
                        log.debug("Order ${order.id}: $ERROR_CONNECTING_MSG_SVC (Payment Success msg), trying again..")
                    } else {
                        log.debug("Order ${order.id}: Error in creating Payment Success messaging request..")
                    }
                    throw exs.removeAt(0)
                }
                if (order.paid == PaymentStatus.TRYING
                    && order.messageSent == MessageSent.NONE_SENT) {
                    val reqId: String? = messagingService.receiveReq(1)
                    order.messageSent = MessageSent.PAYMENT_TRYING
                    log.info("Order ${order.id}: Payment Error message sent successfully, request id: $reqId")
                }
            }, handleError = { o: Order, err: Exception ->
                if ((o.messageSent == MessageSent.NONE_SENT
                            || o.messageSent == MessageSent.PAYMENT_TRYING)
                    && System.currentTimeMillis() - o.createdTime < messageTime) {
                    updateQueue(QueueTask(order, TaskType.MESSAGING, 1))
                    log.info("Order ${order.id}: Error in sending Payment Error message, trying to queue task and add to employee handle..")
                    employeeHandleIssue(order)
                }
            },
                    maxAttempts = numOfRetries,
                    maxDelay = retryDuration,
                    { e -> e is DatabaseUnavailableException }
            ).asyncPerform(messagingService.exceptions, order)
        }
    }
}