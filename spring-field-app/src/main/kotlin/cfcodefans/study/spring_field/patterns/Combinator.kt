package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/combinator
 * Intent:
 *      The functional pattern representing a style of organizing libraries centered around the idea of combining functions.
 *      Putting it simply, there is some type T, some functions for constructing "primitive" values of type T, and some "combinators"
 *      which can combine values of type T in various ways to build up more complex values of type T.
 */
object Combinator {
    private val log: Logger = LoggerFactory.getLogger(Combinator::class.java)

    fun interface IFinder : (String) -> List<String> {
        override fun invoke(text: String): List<String>

        fun not(notFinder: IFinder): IFinder = IFinder { text: String ->
            this(text = text)
                .subtract(notFinder(text).toSet())
                .toList()
        }

        fun or(orFinder: IFinder): IFinder = IFinder { text: String ->
            this(text) + orFinder(text)
        }

        fun and(andFinder: IFinder): IFinder = IFinder { text: String ->
            this(text)
                .intersect(andFinder(text).toSet())
                .toList()
        }
    }

    fun contains(word: String): IFinder = IFinder { text ->
        word.lowercase()
            .let {
                text.lines()
                    .filter { line -> line.lowercase().contains(it) }
                    .toList()
            }
    }

    fun advancedFinder(query: String, orQuery: String, notQuery: String): IFinder = contains(query)
        .or(contains(orQuery))
        .not(contains(notQuery))

    fun filteredFinder(query: String, vararg excludeQueries: String): IFinder {
        return excludeQueries
            .foldRight(contains(query)) { q, finder -> finder.not(contains(q)) }
    }

    private fun identMult(): IFinder = IFinder { text -> text.lines().toList() }

    private fun identSum(): IFinder = IFinder { text -> arrayListOf() }

    fun specializedFinder(vararg queries: String): IFinder {
        return queries.foldRight(identMult()) { q, finder -> finder.and(contains(q)) }
    }

    fun expandedFinder(vararg queries: String): IFinder {
        return queries.foldRight(identSum()) { q, finder -> finder.or(contains(q)) }
    }

    val text: String = """
        It was many and many a year ago,
        In a kingdom by the sea,
        That a maiden there lived whom you may know
        By the name of ANNABEL LEE;
        And this maiden she lived with no other thought
        Than to love and loved by me.
        I was a child and she was a child,
        In this kingdom by the sea;
        But we loved with a love that was more than love-
        I and my Annabel Lee;
        With a love that the winged seraphs of heaven
        Coveted her and me.
    """.trimIndent()

    @JvmStatic
    fun main(args: Array<String>) {
        val queriesOr: Array<String> = arrayOf("many", "Annabel")
        log.info("""the result of expanded(or) query[${queriesOr.joinToString(", ")}] is 
            |${expandedFinder(*queriesOr)(text).joinToString("\n")}""".trimMargin())

        val queriesAnd: Array<String> = arrayOf("Annabel", "my")
        log.info("""the result of specialized(or) query[${queriesAnd.joinToString(", ")}] is 
            |${specializedFinder(*queriesAnd)(text).joinToString("\n")}""".trimMargin())

        log.info("""the result of advanced query is 
            ${advancedFinder("it was", "kingdom", "sea")(text).joinToString("\n")}
        """.trimIndent())

        log.info("""the result of filtered query is
            ${filteredFinder(" was ", "many", "child")(text).joinToString("\n")}
        """.trimIndent())
    }
}