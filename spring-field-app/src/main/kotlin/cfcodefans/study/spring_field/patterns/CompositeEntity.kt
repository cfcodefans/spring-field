package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/composite-entity
 * Intent:
 *      It is used to model, represent, and manage a set of persistent objects that are
 *      interrelated, rather than representing them as individual fine-graded entities.
 */
object CompositeEntity {
    private val log: Logger = LoggerFactory.getLogger(CompositeEntity::class.java)

    /**
     * It is an object, which can contain other dependent objects (there may be a tree of objects within
     * the composite entity), that depends on the coarse-grained object and has its life cycle managed by
     * the coarse-grained object.
     */
    abstract class DependentObj<T>(var data: T?)

    abstract class CoarseGrainedObj<T> {
        open var dependentObjs: Array<DependentObj<T>> = emptyArray()

        open fun getData(): Array<T?> = (dependentObjs.map { d -> d.data } as List<*>).toTypedArray() as Array<T?>
        open fun setData(vararg data: T?): Unit {
            data.slice(dependentObjs.indices).forEachIndexed { i, d -> dependentObjs[i].data = d }
        }
    }

    open class MsgDependentObj(d: String? = null) : DependentObj<String>(d) {}
    open class SignalFDependentObj(d: String? = null) : DependentObj<String>(d) {}

    open class ConsoleCoarseGrainedObj : CoarseGrainedObj<String>() {
        init {
            super.dependentObjs = arrayOf(MsgDependentObj(), SignalFDependentObj())
        }

        override fun getData(): Array<String?> = arrayOf(dependentObjs[0].data, dependentObjs[1].data)
    }

    open class CompositeEntity {
        val console: ConsoleCoarseGrainedObj = ConsoleCoarseGrainedObj()
        open fun getData(): Array<String?> = console.getData()
        fun setData(message: String?, signal: String?) = console.setData(message, signal)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val msg: String = "No Danger"
        val signal: String = "Green Light"
        val console: CompositeEntity = CompositeEntity()
        console.setData(msg, signal)
        console.getData().forEach { log.info(it) }
        console.setData("Danger", "Red Light")
        console.getData().forEach { log.info(it) }
    }
}