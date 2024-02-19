package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/composite
 * Intent:
 *      Compose objects into tree structures to represent part-whole hierarchies.
 *      Composite lets clients treat individual objects and compositions of objects uniformly
 */
object Composite {
    private val log: Logger = LoggerFactory.getLogger(Composite::class.java)

    abstract class LetterComposite {
        private val children: ArrayList<LetterComposite> = arrayListOf()

        fun add(letter: LetterComposite): LetterComposite = apply { children.add(letter) }
        fun count(): Int = children.size
        protected open fun printThisBefore(): LetterComposite = apply { }
        protected open fun printThisAfter(): LetterComposite = apply { }

        open fun print(): LetterComposite = apply {
            printThisBefore()
            children.forEach(LetterComposite::print)
            printThisAfter()
        }
    }

    open class Letter(private val character: Char) : LetterComposite() {
        override fun printThisBefore(): Letter = apply { print(character) }
    }

    open class Word : LetterComposite {
        constructor(letters: List<Letter>) {
            letters.forEach(::add)
        }

        constructor(vararg letters: Char) {
            letters.map { Letter(it) }.forEach(::add)
        }

        override fun printThisBefore(): Word = apply { print(" ") }
    }

    open class Sentence : LetterComposite {
        constructor(words: List<Word>) {
            words.forEach(::add)
        }

        override fun printThisAfter(): Sentence = apply { print(".\n") }
    }

    open class Messenger {
        fun msgFromOrcs(): LetterComposite = Sentence(words = listOf(
                Word(*"Where".toCharArray()),
                Word(*"there".toCharArray()),
                Word(*"is".toCharArray()),
                Word(*"a".toCharArray()),
                Word(*"whip".toCharArray()),
                Word(*"there".toCharArray()),
                Word(*"is".toCharArray()),
                Word(*"a".toCharArray()),
                Word(*"way".toCharArray())))

        fun msgFromElves(): LetterComposite = Sentence(words = listOf(
                Word(*"Much".toCharArray()),
                Word(*"wine".toCharArray()),
                Word(*"pours".toCharArray()),
                Word(*"from".toCharArray()),
                Word(*"your".toCharArray()),
                Word(*"mouth".toCharArray())))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val msger: Messenger = Messenger()
        log.info("Message from the orcs: ")
        msger.msgFromOrcs().print()

        log.info("Message from the elves: ")
        msger.msgFromElves().print()
    }
}