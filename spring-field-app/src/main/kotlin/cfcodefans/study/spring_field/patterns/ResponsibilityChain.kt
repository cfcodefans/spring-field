package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * https://github.com/iluwatar/java-design-patterns/tree/master/chain-of-responsibility
 */
object ResponsibilityChain {
    private val log: Logger = LoggerFactory.getLogger(ResponsibilityChain::class.java)

    enum class ReqType {
        DEFEND_CASTLE, TORTURE_PRISONER, COLLECT_TAX
    }

    open class Request(val reqType: ReqType,
                       val reqDesc: String) {
        var handled: Boolean = false
        fun done(): Request = apply { handled = true }
        override fun toString(): String = reqDesc
    }

    interface IReqHandler {
        fun canHandleReq(req: Request): Boolean
        fun getPriority(): Int
        fun handle(req: Request): Unit
        fun name(): String
    }

    open class OrcCommander : IReqHandler {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(OrcCommander::class.java)
        }

        override fun getPriority(): Int = 2
        override fun canHandleReq(req: Request): Boolean = req.reqType == ReqType.DEFEND_CASTLE
        override fun handle(req: Request) {
            req.done()
            log.info("${name()} handling request \"$req\"")
        }

        override fun name(): String = "Orc commander"
    }

    open class OrcOfficer : IReqHandler {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(OrcSoldier::class.java)
        }

        override fun getPriority(): Int = 3
        override fun canHandleReq(req: Request): Boolean = req.reqType == ReqType.TORTURE_PRISONER
        override fun handle(req: Request) {
            req.done()
            log.info("${name()} handling request \"$req\"")
        }

        override fun name(): String = "Orc officer"
    }

    open class OrcSoldier : IReqHandler {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(OrcSoldier::class.java)
        }

        override fun getPriority(): Int = 1
        override fun canHandleReq(req: Request): Boolean = req.reqType == ReqType.COLLECT_TAX
        override fun handle(req: Request) {
            req.done()
            log.info("${name()} handling request \"$req\"")
        }

        override fun name(): String = "Orc soldier"
    }

    open class OrcKing(vararg reqHandler: IReqHandler) {
        private var handlers: List<IReqHandler> = reqHandler.toList().sortedBy { h -> h.getPriority() }

        fun makeReq(req: Request): OrcKing = apply {
            handlers.firstOrNull { h -> h.canHandleReq(req) }
                ?.handle(req)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val king: OrcKing = OrcKing(OrcCommander(), OrcOfficer(), OrcSoldier())
        king.makeReq(Request(ReqType.DEFEND_CASTLE, "defend castle"))
        king.makeReq(Request(ReqType.TORTURE_PRISONER, "torture prisoner"))
        king.makeReq(Request(ReqType.COLLECT_TAX, "collect tax"))
    }
}