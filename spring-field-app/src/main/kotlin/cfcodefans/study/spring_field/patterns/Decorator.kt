package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/decorator
 * Intent:
 *      Attach additional responsibilities to an object dynamically.
 *      Decorators provide a flexible alternative to subclassing for extending functionality.
 */
object Decorator {
    private val log: Logger = LoggerFactory.getLogger(Decorator::class.java)

    interface ITroll {
        fun attack(): ITroll
        fun getAttackPower(): Int
        fun fleeBattle(): ITroll
    }

    open class ClubbedTroll(private val decorated: ITroll) : ITroll {
        override fun attack(): ClubbedTroll = apply {
            this.decorated.attack()
            log.info("The troll swings at you with a club!")
        }

        override fun getAttackPower(): Int = decorated.getAttackPower() + 30

        override fun fleeBattle(): ClubbedTroll = apply { decorated.fleeBattle() }
    }

    open class SimpleTroll : ITroll {
        override fun attack(): ITroll = apply {
            log.info("The troll tries to grab you!")
        }

        override fun getAttackPower(): Int = 10

        override fun fleeBattle(): SimpleTroll = apply {
            log.info("The troll shrieks in horror and runs away")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Ä simple looking troll approaches.")
        val troll: ITroll = SimpleTroll()
        troll.attack()
            .fleeBattle()
        log.info("Simple troll power: ${troll.getAttackPower()}")

        log.info("A troll with huge club surprises you.")
        val clubTroll: ITroll = ClubbedTroll(troll)
        clubTroll.attack()
            .fleeBattle()
        log.info("Clubbed troll power: ${clubTroll.getAttackPower()}")
    }
}