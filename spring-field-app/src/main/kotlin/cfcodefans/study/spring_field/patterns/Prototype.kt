package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Objects

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/prototype
 * Intent:
 *      Specify the kinds of objects to create using a prototypical instance.
 *      and create new objects by copying this prototype.
 */
object Prototype {
    private val log: Logger = LoggerFactory.getLogger(Prototype::class.java)

    abstract class Prototype<T> : Cloneable {
        open fun copy(): T = super.clone() as T
    }

    abstract class Beast(source: Beast?) : Prototype<Beast>()

    abstract class Mage(source: Mage?) : Prototype<Mage>()

    abstract class Warlord(source: Warlord?) : Prototype<Warlord>()


    open class ElfBeast(private val helpType: String?, elfBeast: ElfBeast? = null) : Beast(elfBeast) {
        constructor(elfBeast: ElfBeast?) : this(helpType = elfBeast?.helpType, elfBeast)

        override fun equals(other: Any?): Boolean = other is ElfBeast && other.helpType == this.helpType
        override fun hashCode(): Int = Objects.hash(ElfBeast::class, super.hashCode(), helpType)
        override fun toString(): String = "Elven eagle helps in $helpType"
    }

    open class ElfMage(private val helpType: String?, elfMage: ElfMage? = null) : Mage(elfMage) {
        constructor(elfMage: ElfMage?) : this(helpType = elfMage?.helpType, elfMage)

        override fun equals(other: Any?): Boolean = other is ElfMage && other.helpType == this.helpType
        override fun hashCode(): Int = Objects.hash(ElfMage::class, super.hashCode(), helpType)
        override fun toString(): String = "Elven mage helps in $helpType"
    }

    open class ElfWarlord(private val helpType: String?, elfWarlord: ElfWarlord? = null) : Warlord(elfWarlord) {
        constructor(elfWarlord: ElfWarlord?) : this(helpType = elfWarlord?.helpType, elfWarlord)

        override fun equals(other: Any?): Boolean = other is ElfWarlord && other.helpType == this.helpType
        override fun hashCode(): Int = Objects.hash(ElfWarlord::class, super.hashCode(), helpType)
        override fun toString(): String = "Elven warlord helps in $helpType"
    }

    open class OrcBeast(private val weapon: String?, orcBeast: OrcBeast? = null) : Beast(orcBeast) {
        constructor(beast: OrcBeast?) : this(weapon = beast?.weapon, beast)

        override fun equals(other: Any?): Boolean = other is OrcBeast && other.weapon == this.weapon
        override fun hashCode(): Int = Objects.hash(OrcBeast::class, weapon)
        override fun toString(): String = "Orcish wolf attacks with $weapon"
    }

    open class OrcMage(private val weapon: String?, orcMage: OrcMage? = null) : Mage(orcMage) {
        constructor(mage: OrcMage?) : this(weapon = mage?.weapon, mage)

        override fun equals(other: Any?): Boolean = other is OrcMage && other.weapon == this.weapon
        override fun hashCode(): Int = Objects.hash(OrcMage::class, weapon)
        override fun toString(): String = "Orcish mage attacks with $weapon"
    }

    open class OrcWarlord(private val weapon: String?, orcWarlord: OrcWarlord? = null) : Warlord(orcWarlord) {
        constructor(mage: OrcWarlord?) : this(weapon = mage?.weapon, mage)

        override fun equals(other: Any?): Boolean = other is OrcWarlord && other.weapon == this.weapon
        override fun hashCode(): Int = Objects.hash(OrcWarlord::class, weapon)
        override fun toString(): String = "Orcish warlord attacks with $weapon"
    }

    interface IHeroFactory {
        fun createMage(): Mage
        fun createWarlord(): Warlord
        fun createBeast(): Beast
    }

    open class HeroFactory(private val mage: Mage,
                           private val warlord: Warlord,
                           private val beast: Beast) : IHeroFactory {
        override fun createMage(): Mage = mage.copy()
        override fun createWarlord(): Warlord = warlord.copy()
        override fun createBeast(): Beast = beast.copy()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        var factory: IHeroFactory = HeroFactory(mage = ElfMage("cooking"),
                warlord = ElfWarlord("cleaning"),
                beast = ElfBeast("protecting"))

        log.info(factory.createMage().toString())
        log.info(factory.createWarlord().toString())
        log.info(factory.createBeast().toString())

        factory = HeroFactory(beast = OrcBeast("laser"),
                mage = OrcMage("axe"),
                warlord = OrcWarlord("sword"))
        log.info(factory.createMage().toString())
        log.info(factory.createWarlord().toString())
        log.info(factory.createBeast().toString())
    }
}