package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/context-object
 * Intent
 *      Decouple data from protocol-specific classes and store the scoped data in an object
 *      independent of the underlying protocol technology.
 */
object ContextObject {
    private val log: Logger = LoggerFactory.getLogger(ContextObject::class.java)

    data class ServiceCtx(var accountService: String? = null,
                          var sessionService: String? = null,
                          var searchService: String? = null)

    fun createCtx(): ServiceCtx = ServiceCtx()

    open class LayerA(val ctx: ServiceCtx = createCtx()) {
        open fun addAccountInfo(accountService: String?): Unit {
            ctx.accountService = accountService
        }
    }

    open class LayerB(layerA: LayerA) {
        var ctx: ServiceCtx = layerA.ctx
            private set

        open fun addSessionInfo(sessionService: String?): Unit {
            ctx.sessionService = sessionService
        }
    }

    open class LayerC(layerB: LayerB) {
        var ctx: ServiceCtx = layerB.ctx
            private set

        open fun addSearchInfo(searchService: String?): Unit {
            ctx.searchService = searchService
        }
    }

    const val SERVICE: String = "SERVICE"

    /**
     * In the context object pattern, information and data from underlying protocol-specific classes/systems is decoupled
     * and stored into a protocol-independent object in an organised format, The pattern ensures the data contained within the context object
     * can be shared and further structured between different layers of a software system.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val la: LayerA = LayerA()
        la.addAccountInfo(SERVICE)
        log.info("Context = ${la.ctx}")

        val lb: LayerB = LayerB(la)
        lb.addSessionInfo(SERVICE)
        log.info("Context = ${lb.ctx}")

        val lc: LayerC = LayerC(lb)
        lc.addSearchInfo(SERVICE)
        log.info("Context = ${lc.ctx}")
    }
}