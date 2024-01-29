package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Decouple an abstraction from its implementation so that the two can vary independently.
 */
object Bridge {
    private val log: Logger = LoggerFactory.getLogger(Bridge::class.java)

    interface IEnchantment {
        fun onActivate()
        fun apply()
        fun onDeactivate()
    }

    open class FlyingEnchantment : IEnchantment {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(FlyingEnchantment::class.java)
        }

        override fun onActivate() = log.info("The item begins to glow faintly.")
        override fun apply() = log.info("The item flies and strikes the enemies finally returning to owner")
        override fun onDeactivate() = log.info("The item's glow fades")
    }

    open class SoulEatingEnchantment : IEnchantment {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(SoulEatingEnchantment::class.java)
        }

        override fun onActivate() = log.info("The item spreads bloodlust.")
        override fun apply() = log.info("The item eats the soul of enemies.")
        override fun onDeactivate() = log.info("Bloodlust slowly disappears.")
    }

    interface IWeapon {
        fun wield()
        fun swing()
        fun unwield()
        val enchantment: IEnchantment
    }

    open class Hammer(override val enchantment: IEnchantment) : IWeapon {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(Hammer::class.java)
        }

        override fun wield() {
            log.info("The hammer is wielded.")
            enchantment.onActivate()
        }

        override fun swing() {
            log.info("The hammer is swung.")
            enchantment.apply()
        }

        override fun unwield() {
            log.info("The hammer is unwielded")
            enchantment.onDeactivate()
        }
    }

    open class Sword(override val enchantment: IEnchantment) : IWeapon {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(Sword::class.java)
        }

        override fun wield() {
            log.info("The sword is wielded.")
            enchantment.onActivate()
        }

        override fun swing() {
            log.info("The sword is swung.")
            enchantment.apply()
        }

        override fun unwield() {
            log.info("The sword is unwielded")
            enchantment.onDeactivate()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("The knight receives an enchanted sword.")
        val enchantedSword: Sword = Sword(SoulEatingEnchantment())
        enchantedSword.wield()
        enchantedSword.swing()
        enchantedSword.unwield()

        log.info("The valkyrie receives an enchanted hammer.")
        val hammer: Hammer = Hammer(FlyingEnchantment())
        hammer.wield()
        hammer.swing()
        hammer.unwield()
    }
}