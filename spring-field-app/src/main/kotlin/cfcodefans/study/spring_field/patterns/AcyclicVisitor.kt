package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/acyclic-visitor
 */
object AcyclicVisitor {
    private val log: Logger = LoggerFactory.getLogger("AcyclicVisitor")

    interface IModem {
        fun accept(modemVisitor: IModemVisitor)
    }

    interface IModemVisitor {}

    class Zoom : IModem {
        override fun toString(): String = "Zoom modem"
        override fun accept(modemVisitor: IModemVisitor) {
            if (modemVisitor is IZoomVisitor) modemVisitor.visit(this)
            else log.info("Only ZoomVisitor is allowed to visit Zoom modem")
        }
    }

    interface IZoomVisitor : IModemVisitor {
        fun visit(zoom: Zoom)
    }

    class Hayes : IModem {
        override fun toString(): String = "Hayes modem"
        override fun accept(modemVisitor: IModemVisitor) {
            if (modemVisitor is IHayesVisitor) modemVisitor.visit(this)
            else log.info("Only IHayesVisitor is allowed to visit Zoom modem")
        }
    }

    interface IHayesVisitor : IModemVisitor {
        fun visit(hayes: Hayes)
    }

    interface IAllModemVisitor : IZoomVisitor, IHayesVisitor {}

    class ConfiguredForDosVisitor : IAllModemVisitor {
        override fun visit(zoom: Zoom) {
            log.info("$zoom used with Dos configurator.")
        }

        override fun visit(hayes: Hayes) {
            log.info("$hayes used with Dos configurator.")
        }
    }

    class ConfigureForUnixVisitor : IZoomVisitor {
        override fun visit(zoom: Zoom) {
            log.info("$zoom used with Unix configurator.")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val conUnix: IModemVisitor = ConfigureForUnixVisitor()
        val conDos: IModemVisitor = ConfiguredForDosVisitor()

        val zoom: IModem = Zoom()
        val hayes: IModem = Hayes()

        hayes.accept(conDos)
        zoom.accept(conDos)
        hayes.accept(conUnix)
        zoom.accept(conUnix)
    }
}