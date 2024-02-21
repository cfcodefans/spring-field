package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * refers to https://github.com/iluwatar/java-design-patterns/blob/master/event-aggregator/src/main/java/com/iluwatar/event/aggregator/Weekday.java
 * Intent:
 *      A system with lots of objects can lead to complexities when a client wants to
 *      subscribe to events. The client has to find and register for each object individually.
 *      If each object has multiple events then each event requires a separate subscription.
 *      An Event Aggregator acts as a single source of events for many objects.
 *      it registers for all the events of the many objects allowing clients to register with just the aggregator.
 */
object EventAggregator {
    private val log: Logger = LoggerFactory.getLogger(EventAggregator::class.java)

    enum class Weekday(private val description: String) {
        MONDAY("Monday"),
        TUESDAY("Tuesday"),
        WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"),
        FRIDAY("Friday"),
        SATURDAY("Saturday"),
        SUNDAY("Sunday");

        override fun toString(): String = description
    }

    enum class Event(private val description: String) {
        WHITE_WALKERS_SIGHTED("White walkers sighted"),
        STARK_SIGHTED("Stark sighted"),
        WARSHIPS_APPROACHING("Warship approaching"),
        TRAITOR_DETECTED("Traitor detected");

        override fun toString(): String = description
    }

    fun interface IEventObserver {
        fun onEvent(ev: Event)
    }

    abstract class EventEmitter(obs: IEventObserver?, e: Event?) {
        private val observerList: MutableMap<Event, MutableList<IEventObserver>> = EnumMap(Event::class.java)

        init {
            if (obs != null && e != null)
                registerObserver(obs, e)
        }

        fun registerObserver(obs: IEventObserver, e: Event): EventEmitter = apply {
            observerList
                .computeIfAbsent(e) { LinkedList() }
                .let { if (it.contains(obs).not()) it.add(obs) }
        }

        fun notifyObservers(e: Event): EventEmitter = apply {
            observerList[e]
                ?.forEach() { ob -> ob.onEvent(e) }
        }

        abstract fun timePasses(day: Weekday): Unit
    }

    open class Scout(obs: IEventObserver? = null, e: Event? = null) : EventEmitter(obs, e) {
        constructor() : this(null, null)

        override fun timePasses(day: Weekday) {
            if (day == Weekday.TUESDAY) notifyObservers(Event.WARSHIPS_APPROACHING)
            if (day == Weekday.WEDNESDAY) notifyObservers(Event.WHITE_WALKERS_SIGHTED)
        }
    }

    open class LordVars(obs: IEventObserver? = null, e: Event? = null) : EventEmitter(obs, e), IEventObserver {
        constructor() : this(null, null)

        override fun timePasses(day: Weekday) {
            if (day == Weekday.SATURDAY) notifyObservers(Event.TRAITOR_DETECTED)
        }

        override fun onEvent(ev: Event): Unit {
            notifyObservers(ev)
        }
    }

    open class LordBaelish(obs: IEventObserver? = null, e: Event? = null) : EventEmitter(obs, e) {
        constructor() : this(null, null)

        override fun timePasses(day: Weekday) {
            if (day == Weekday.FRIDAY) notifyObservers(Event.STARK_SIGHTED)
        }
    }

    open class KingsHand(obs: IEventObserver? = null, e: Event? = null) : EventEmitter(obs, e), IEventObserver {
        constructor() : this(null, null)

        override fun timePasses(day: Weekday) {}

        override fun onEvent(ev: Event): Unit {
            notifyObservers(ev)
        }
    }

    open class KingJoffrey : IEventObserver {
        override fun onEvent(ev: Event) {
            log.info("Received event from the king's Hand: $ev")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val kingJoffery: KingJoffrey = KingJoffrey()
        val kingsHand: KingsHand = KingsHand().apply {
            registerObserver(kingJoffery, Event.TRAITOR_DETECTED)
            registerObserver(kingJoffery, Event.STARK_SIGHTED)
            registerObserver(kingJoffery, Event.WARSHIPS_APPROACHING)
            registerObserver(kingJoffery, Event.WHITE_WALKERS_SIGHTED)
        }

        val varys: LordVars = LordVars().apply {
            registerObserver(kingsHand, Event.TRAITOR_DETECTED)
            registerObserver(kingsHand, Event.WHITE_WALKERS_SIGHTED)
        }

        val scout: Scout = Scout().apply {
            registerObserver(kingsHand, Event.WARSHIPS_APPROACHING)
            registerObserver(varys, Event.WHITE_WALKERS_SIGHTED)
        }

        val baelish: LordBaelish = LordBaelish(kingsHand, Event.STARK_SIGHTED)

        val emitters: List<EventEmitter> = listOf(kingsHand, baelish, varys, scout)

        Weekday.entries
            .map { day -> { emitter: EventEmitter -> emitter.timePasses(day) } }
            .forEach { action -> emitters.forEach() { emitter -> action(emitter) } }
    }
}