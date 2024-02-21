package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/model-view-controller
 * Intent:
 *      separate the user interface into three interconnected components: the model, the view and the controller.
 *      Let the model manage the data, the view display the data,
 *      and the controller mediate updating the data and redrawing the display
 */
object ModelViewController {
    private val log: Logger = LoggerFactory.getLogger(ModelViewController::class.java)

    enum class Nourishment(private val title: String) {
        SATURATED("saturated"),
        HUNGRY("hungry"),
        STARVING("starving");

        override fun toString(): String = title
    }

    enum class Health(private val title: String) {
        HEALTHY("healthy"),
        WOUNDED("wounded"),
        DEAD("dead");

        override fun toString(): String = title
    }

    enum class Fatigue(private val title: String) {
        ALERT("alert"),
        TIRED("tired"),
        SLEEPING("sleeping");

        override fun toString(): String = title
    }

    data class GiantModel(var health: Health,
                          var fatigue: Fatigue,
                          var nourishment: Nourishment) {
        override fun toString(): String = "The giant looks $health, $fatigue and $nourishment"
    }

    open class GiantView {
        fun displayGiant(giant: GiantModel): Unit = log.info(giant.toString())
    }

    open class GiantController(private val giant: GiantModel,
                               private val view: GiantView) {
        var health: Health
            get() = giant.health
            set(value) {
                giant.health = value
            }

        var fatigue: Fatigue
            get() = giant.fatigue
            set(value) {
                giant.fatigue = value
            }

        var nourishment: Nourishment
            get() = giant.nourishment
            set(value) {
                giant.nourishment = value
            }

        fun updateView() = view.displayGiant(giant)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val giant: GiantModel = GiantModel(health = Health.HEALTHY,
                fatigue = Fatigue.ALERT,
                nourishment = Nourishment.SATURATED)

        val view: GiantView = GiantView()
        var controller: GiantController = GiantController(giant, view)
        controller.updateView()

        controller.health = Health.WOUNDED
        controller.nourishment = Nourishment.HUNGRY
        controller.fatigue = Fatigue.TIRED

        controller.updateView()
    }
}