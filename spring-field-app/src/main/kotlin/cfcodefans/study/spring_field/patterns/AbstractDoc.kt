package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory


object AbstractDocument {

    private val log: Logger = LoggerFactory.getLogger("AbstractDocument")

    /**
     * refers to https://github.com/iluwatar/java-design-patterns/blob/master/abstract-document/src/main/java/com/iluwatar/abstractdocument/AbstractDocument.java
     */

    /**
     * Document interface
     */
    interface IDoc {
        /**
         * puts the value related to the key
         * @param key element ky
         * @param value element value
         * @return Unit
         */
        fun put(key: String, value: Any)

        /**
         * Gets the value for the key.
         *
         * @param key element key
         * @return value or null
         */
        fun get(key: String?): Any?

        /**
         * Gets the stream of child documents.
         *
         * @param key         element key
         * @param constructor constructor of child class
         * @return child documents
         */
        fun <T> children(key: String?, constructor: (TProps) -> T): Sequence<T>?
    }

    abstract class AbstractDoc(
            private val properties: MutableMap<String, Any?>
    ) : IDoc {
        override fun put(key: String, value: Any) = run { properties[key] = value }
        override fun get(key: String?): Any? = properties[key]

        override fun <T> children(key: String?, constructor: (TProps) -> T): Sequence<T>? =
            get(key)
                ?.let { el -> el as List<TProps> }
                ?.asSequence()
                ?.map(constructor)

        override fun toString(): String = "${this.javaClass.name}[${properties.entries.map { en -> "[${en.key} : ${en.value}]" }}]"

    }

    enum class Property {
        PARTS, TYPE, PRICE, MODEL
    }

    interface IHasModel : IDoc {
        fun getModel(): String? = this.get(Property.MODEL.name)?.toString()
    }

    interface IHasType : IDoc {
        fun getType(): String? = this.get(Property.TYPE.name)?.toString()
    }

    interface IHasPrice : IDoc {
        fun getPrice(): Number? = this.get(Property.PRICE.name) as Number?
    }

    interface IHasParts : IDoc {
        fun getParts(): Sequence<Part>? = children(key = Property.PARTS.toString(), constructor = ::Part)
    }

    class Part(props: TProps)
        : AbstractDoc(props), IHasType, IHasModel, IHasPrice {

    }

    class Car(props: TProps)
        : AbstractDoc(props), IHasParts, IHasModel, IHasPrice {

    }

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Constructing parts and car")

        val wheelProps: TProps = mutableMapOf<String, Any?>(Property.TYPE.toString() to "wheel",
                Property.MODEL.toString() to "15C",
                Property.PRICE.toString() to 100)

        val doorProps: TProps = mutableMapOf(
                Property.TYPE.toString() to "door",
                Property.MODEL.toString() to "Lambo",
                Property.PRICE.toString() to 300L)

        val carProps: TProps = mutableMapOf(
                Property.MODEL.toString() to "300SL",
                Property.PRICE.toString() to 10000L,
                Property.PARTS.toString() to listOf(wheelProps, doorProps))

        val car: Car = Car(carProps)
        log.info("Here is our car: ")
        log.info("-> model: ${car.getModel()}")
        log.info("-> price: ${car.getPrice()}")
        log.info("-> parts: ")
        car.getParts()?.forEach { p -> log.info("\t${p.getType()}/${p.getModel()}/${p.getPrice()}") }
    }
}

typealias TProps = MutableMap<String, Any?>
