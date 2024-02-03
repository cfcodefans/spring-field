package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Adapter {
    private val log: Logger = LoggerFactory.getLogger("Adapter")

    interface IRowingBoat {
        fun row()
    }

    class FishingBoat {
        fun sail() = log.info("The fishing boat is sailing")
    }

    open class Fishing2RowingBoatAdapter(private val boat: FishingBoat = FishingBoat()) : IRowingBoat {
        override fun row() = boat.sail()
    }

    class Caption(private val rowingBoat: IRowingBoat) {
        fun row() = rowingBoat.row()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        FishingBoat()
            .let { Fishing2RowingBoatAdapter(it) }
            .let { Caption(rowingBoat = it) }
            .row()
    }
}