package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/flyweight
 * Intent:
 *      Use sharing to support large numbers of fine-grained objects efficiently.
 */
object Flyweight {
    private val log: Logger = LoggerFactory.getLogger(Flyweight::class.java)

    fun interface IPotion {
        fun drink(): Unit
    }

    enum class PotionType {
        HEALING, INVISIBILITY, STRENGTH, HOLY_WATER, POISON
    }

    open class HealingPotion : IPotion {
        override fun drink() = log.info("You feel healed. (Potion=${System.identityHashCode(this)})")
    }

    open class HolyWaterPotion : IPotion {
        override fun drink() = log.info("You feel blessed. (Potion=${System.identityHashCode(this)})")
    }

    open class InvisibilityPotion : IPotion {
        override fun drink() = log.info("You become invisible. (Potion=${System.identityHashCode(this)})")
    }

    open class PoisonPotion : IPotion {
        override fun drink() = log.info("Urgh! This is poisonous. (Potion=${System.identityHashCode(this)})")
    }

    open class StrengthPotion : IPotion {
        override fun drink() = log.info("You feel strong. (Potion=${System.identityHashCode(this)})")
    }

    open class PotionFactory {
        private val potions: MutableMap<PotionType, IPotion> = EnumMap(PotionType::class.java)

        fun createPotion(type: PotionType): IPotion = potions
            .computeIfAbsent(type) { type ->
                when (type) {
                    PotionType.HEALING -> HealingPotion()
                    PotionType.HOLY_WATER -> HolyWaterPotion()
                    PotionType.INVISIBILITY -> InvisibilityPotion()
                    PotionType.POISON -> PoisonPotion()
                    PotionType.STRENGTH -> StrengthPotion()
                }
            }
    }

    val factory: PotionFactory = PotionFactory()

    open class AlchemistShop {
        private val topShelf: List<IPotion> = listOf(
                factory.createPotion(PotionType.INVISIBILITY),
                factory.createPotion(PotionType.INVISIBILITY),
                factory.createPotion(PotionType.STRENGTH),
                factory.createPotion(PotionType.HEALING),
                factory.createPotion(PotionType.INVISIBILITY),
                factory.createPotion(PotionType.STRENGTH),
                factory.createPotion(PotionType.HEALING),
                factory.createPotion(PotionType.HEALING))

        private val bottomShelf: List<IPotion> = listOf(
                factory.createPotion(PotionType.POISON),
                factory.createPotion(PotionType.POISON),
                factory.createPotion(PotionType.POISON),
                factory.createPotion(PotionType.HOLY_WATER),
                factory.createPotion(PotionType.HOLY_WATER))

        fun getTopShelf(): List<IPotion> = this.topShelf.toList()

        fun getBottomShelf(): List<IPotion> = this.bottomShelf.toList()

        fun drinkPotions() {
            log.info("Drinking top shelf potions")
            topShelf.forEach(IPotion::drink)
            log.info("Drinking bottom shelf potions")
            bottomShelf.forEach(IPotion::drink)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {

        // create the alchemist shop with the potions
        val alchemistShop = AlchemistShop()
        // a brave visitor enters the alchemist shop and drinks all the potions
        alchemistShop.drinkPotions()
    }
}