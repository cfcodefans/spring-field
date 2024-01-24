package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object AbstractFactory {
    private val log: Logger = LoggerFactory.getLogger("AbstractFactory")

    interface IArmy {
        val description: String
    }

    interface ICastle {
        val description: String
    }

    interface IKing {
        val description: String
    }


    data class Kingdom(var king: IKing, var castle: ICastle, var army: IArmy)

    enum class KingdomType { ELF, ORC }

    interface IKingdomFactory {
        fun createCastle(): ICastle
        fun createKing(): IKing
        fun createArmy(): IArmy
    }

    class ElfArmy : IArmy {
        override val description: String = "This is the elven army!"
    }

    class ElfCastle : ICastle {
        override val description: String = "This is the elven castle!"
    }

    class ElfKing : IKing {
        override val description: String = "This is the elven king!"
    }

    class ElfKingdomFactory : IKingdomFactory {
        override fun createArmy(): IArmy = ElfArmy()
        override fun createCastle(): ICastle = ElfCastle()
        override fun createKing(): IKing = ElfKing()
    }


    class OrcArmy : IArmy {
        override val description: String = "This is the orc army!"
    }

    class OrcCastle : ICastle {
        override val description: String = "This is the orc castle!"
    }

    class OrcKing : IKing {
        override val description: String = "This is the orc king!"
    }

    class OrcKingdomFactory : IKingdomFactory {
        override fun createArmy(): IArmy = OrcArmy()
        override fun createCastle(): ICastle = OrcCastle()
        override fun createKing(): IKing = OrcKing()
    }

    object FactoryMaker {
        @JvmStatic
        fun makeFactory(type: KingdomType?): IKingdomFactory = when (type) {
            KingdomType.ELF -> ElfKingdomFactory()
            KingdomType.ORC -> OrcKingdomFactory()
            else -> throw IllegalArgumentException("KingdomType: $type not supported.")
        }
    }

    fun createKingdom(type: KingdomType): Kingdom = FactoryMaker
        .makeFactory(type)
        .let { factory -> Kingdom(factory.createKing(), factory.createCastle(), factory.createArmy()) }


    @JvmStatic
    fun main(args: Array<String>) {
        log.info("elf kingdom")
        var kingdom: Kingdom = createKingdom(KingdomType.ELF)
        log.info("""
            ${kingdom.army.description}
            ${kingdom.castle.description}
            ${kingdom.king.description}
        """.trimIndent())

        log.info("orc kingdom")
        kingdom = createKingdom(KingdomType.ORC)
        log.info("""
            ${kingdom.army.description}
            ${kingdom.castle.description}
            ${kingdom.king.description}
        """.trimIndent())
    }
}