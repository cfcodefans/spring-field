package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue

/**
 * Refers to https://github.com/iluwatar/java-design-patterns/tree/master/collecting-parameter
 * Intent:
 *      To store the collaborative result of numerous methods within a collection
 */
object CollectingParameters {
    private val log: Logger = LoggerFactory.getLogger(CollectingParameters::class.java)

    enum class PaperSize {
        A2, A3, A4
    }

    data class PrinterItem(var paperSize: PaperSize,
                           var pageCount: Int,
                           var isDoubleSided: Boolean,
                           var isColor: Boolean) {
        init {
            if (pageCount < 1) throw IllegalArgumentException("invalid pageCount: $pageCount")
        }
    }

    open class PrinterQueue {
        val printerItemQueue: Queue<PrinterItem> = LinkedList()
        open fun addPrinterItem(printerItem: PrinterItem): PrinterQueue = apply {
            printerItemQueue.add(printerItem)
        }

        open fun emptyQueue(): PrinterQueue = apply { printerItemQueue.clear() }
    }

    val printerQueue: PrinterQueue by lazy { PrinterQueue() }

    /**
     * Adds A4 document jobs to the collecting parameter according to some policy that can be whatever
     * the client (the print center) wants
     * @param printerItemsCollection the collecting parameter
     */
    open fun addValidA4Papers(printerItemCollection: Queue<PrinterItem>): Unit {
        printerQueue.printerItemQueue
            .filter { nextItem ->
                nextItem.paperSize == PaperSize.A4
                        && ((nextItem.isColor && nextItem.isDoubleSided.not())
                        || nextItem.isColor.not())
            }.forEach { printerItemCollection.add(it) }
    }

    open fun addValidA3Papers(printerItemCollection: Queue<PrinterItem>): Unit {
        printerQueue.printerItemQueue
            .filter { nextItem ->
                nextItem.paperSize == PaperSize.A3
                        && nextItem.isColor.not()
                        && nextItem.isDoubleSided.not()
            }.forEach { printerItemCollection.add(it) }
    }

    open fun addValidA2Papers(printerItemCollection: Queue<PrinterItem>): Unit {
        printerQueue.printerItemQueue
            .filter { nextItem ->
                nextItem.paperSize == PaperSize.A2
                        && nextItem.pageCount == 1
                        && nextItem.isDoubleSided.not()
                        && nextItem.isColor.not()
            }.forEach { printerItemCollection.add(it) }
    }

    /**
     * The Collecting parameter Design Pattern aims to return a result that is the collaborative result of several methods.
     * This design pattern uses a "Collecting parameter" that is passed to several functions, accumulating results
     * as it travels from method to method. This is different to the composed Method design pattern, where a single
     * collection is modified via several methods.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        printerQueue.addPrinterItem(PrinterItem(PaperSize.A4, 5, false, false))
            .addPrinterItem(PrinterItem(PaperSize.A3, 2, false, false))
            .addPrinterItem(PrinterItem(PaperSize.A2, 5, false, false))

        //This variable is the collecting parameter, and will store the policy abiding print jobs
        var result = LinkedList<PrinterItem>()

        /*  Adding A4, A3, and A2 papers that obey the policy */
        addValidA4Papers(result)
        addValidA3Papers(result)
        addValidA2Papers(result)

        log.info("result: \n${result.joinToString("\n")}")
    }
}