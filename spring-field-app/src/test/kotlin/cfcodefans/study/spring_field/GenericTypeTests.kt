package cfcodefans.study.spring_field

import com.thenetcircle.commons.Jsons
import java.util.Date

abstract class ObjHolder<T>(var obj: T,
                            var name: String,
                            var attr: MutableMap<String, Any> = HashMap()) {
    override fun toString(): String = Jsons.toString(mapOf(
        "clz" to this.javaClass.simpleName,
        "obj" to obj,
        "name" to name,
        "attr" to attr
    ))
}

open class DateHolder(date: Date,
                      name: String,
                      attr: MutableMap<String, Any>) : ObjHolder<Date>(obj = date, name = name, attr = attr) {

}

open class IntHolder(value: Int, name: String) : ObjHolder<Int>(obj = value, name = name)

interface IObjOper<OH : ObjHolder<*>> {
    fun perform(oh: OH): OH
}

interface IStampOper<OH : ObjHolder<*>> : IObjOper<OH> {
    var dateProvider: () -> Date
    fun perform(oh: OH, logTime: Date): OH = super.perform(oh).also { it.attr["updatedAt"] = logTime }
    override fun perform(oh: OH): OH = perform(oh, dateProvider())
}

open class LogOper<OH : ObjHolder<*>>(val name: String) : IStampOper<OH> {
    override var dateProvider: () -> Date = { Date() }
    override fun perform(oh: OH): OH = oh.also { it.name = name; super.perform(it) }
}

open class GenericTypeTests {
    open fun testGenericType_1() {
        val oh: IntHolder = IntHolder(value = 42, name = "unknown")
        LogOper<IntHolder>(name = "answer of universe")
            .perform(oh = oh)
    }
}