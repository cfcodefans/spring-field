package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/memento
 * Intent:
 *      Without violating encapsulation, capture and externalize an object's internal state
 *      so that the object can be restored to this state later.
 */
object Memento {
    private val log: Logger = LoggerFactory.getLogger(Memento::class.java)

    enum class StarType(private val title: String) {
        SUN("sun"),
        RED_GIANT("red giant"),
        WHITE_DWARF("white dwarf"),
        SUPERNOVA("supernova"),
        DEAD("dead start");

        override fun toString(): String = title
    }

    interface IStarMemento {}

    data class StarMementoInternal(var type: StarType,
                                   var ageYears: Int,
                                   var massTons: Int) : IStarMemento

    /**
     * Star uses "mementos" to store and restore state.
     */
    open class Star(private var type: StarType,
                    private var ageYears: Int,
                    private var massTons: Int) {
        fun timePasses(): Star = apply {
            ageYears *= 2
            massTons *= 8
            type = when (type) {
                StarType.RED_GIANT -> StarType.WHITE_DWARF
                StarType.SUN -> StarType.RED_GIANT
                StarType.SUPERNOVA -> StarType.DEAD
                StarType.WHITE_DWARF -> StarType.SUPERNOVA
                StarType.DEAD -> {
                    ageYears *= 2
                    massTons = 0
                    StarType.DEAD
                }
            }
        }

        fun getMemento(): IStarMemento = StarMementoInternal(type = type,
                massTons = massTons,
                ageYears = ageYears)

        fun setMemento(memento: IStarMemento): Star = apply {
            val state: StarMementoInternal = memento as StarMementoInternal

            type = state.type
            ageYears = state.ageYears
            massTons = state.massTons
        }

        override fun toString(): String = "$type age: $ageYears years, mass: $massTons tons"
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val states: Stack<IStarMemento> = Stack<IStarMemento>()

        val star = Star(StarType.SUN, 10000000, 500000)
        log.info(star.toString())
        states.add(star.getMemento())
        star.timePasses()
        log.info(star.toString())
        states.add(star.getMemento())
        star.timePasses()
        log.info(star.toString())
        states.add(star.getMemento())
        star.timePasses()
        log.info(star.toString())
        states.add(star.getMemento())
        star.timePasses()
        log.info(star.toString())
        while (states.size > 0) {
            star.setMemento(states.pop())
            log.info(star.toString())
        }
    }
}