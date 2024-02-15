package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Deque
import java.util.LinkedList

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/command
 * Intent:
 *      Encapsulate a request as an object, thereby letting you parameterize clients with different requests,
 *      queue or log requests, and support undoable operations
 */
object Command {
    private val log: Logger = LoggerFactory.getLogger(Command::class.java)

    enum class Size(private val title: String) {
        SMALL("small"), NORMAL("normal");

        override fun toString(): String = title
    }

    enum class Visibility(private val title: String) {
        VISIBLE("visible"), INVISIBLE("invisible");

        override fun toString(): String = title
    }

    abstract class Target(protected var size: Size,
                          protected var visibility: Visibility) {
        open fun info(): Unit = log.info("$this, [size = $size] [visibility = $visibility]")
        open fun changeSize() = apply {
            size = if (size == Size.NORMAL) Size.SMALL else Size.NORMAL
        }

        open fun changeVisibility() = apply {
            visibility = if (visibility == Visibility.INVISIBLE) Visibility.VISIBLE else Visibility.INVISIBLE
        }
    }

    open class Goblin : Target(size = Size.NORMAL, visibility = Visibility.VISIBLE) {
        override fun toString(): String = "Goblin"
    }

    open class Wizard {
        override fun toString(): String = "Wizard"
        private val undoStack: Deque<Runnable> = LinkedList()
        private val redoStack: Deque<Runnable> = LinkedList()

        open fun castSpell(runnable: Runnable): Wizard = apply {
            runnable.run()
            undoStack.offerLast(runnable)
        }

        open fun undoLastSpell(): Wizard = apply {
            if (undoStack.isEmpty()) return@apply
            undoStack.pollLast().also {
                redoStack.offerLast(it)
                it.run()
            }
        }

        open fun redoLastSpell(): Wizard = apply {
            if (redoStack.isEmpty()) return@apply
            redoStack.pollLast().also {
                undoStack.offerLast(it)
                it.run()
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val wizard: Wizard = Wizard()
        val goblin: Target = Goblin()

        goblin.info()

        wizard.castSpell(goblin::changeSize)
        goblin.info()

        wizard.castSpell(goblin::changeVisibility)
        goblin.info()

        wizard.undoLastSpell()
        goblin.info()

        wizard.undoLastSpell()
        goblin.info()

        wizard.redoLastSpell()
        goblin.info()

        wizard.redoLastSpell()
        goblin.info()
    }
}