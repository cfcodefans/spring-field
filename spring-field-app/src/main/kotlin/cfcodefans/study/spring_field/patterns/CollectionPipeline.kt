package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/collection-pipeline
 *
 * Intent
 * Collection Pipeline introduces Function Composition and Collection Pipeline, two functional-style patterns
 * that you can combine to iterate collections in your code. In functional Programming, it's common to sequence
 * complex operations through a series of smaller modular functions or operations. The series is called a composition of functions,
 * or a function composition. When a collection of data flows through a function composition, it becomes a collection pipeline.
 * Function Composition and Collection Pipeline are two design patterns frequently used in functional-style programming
 */
object CollectionPipeline {
    private val log: Logger = LoggerFactory.getLogger(CollectionPipeline::class.java)

    enum class Category {
        JEEP, SEDAN, CONVERTIBLE
    }

    data class Car(val make: String,
                   val model: String,
                   val year: Int,
                   val category: Category)

    fun createCars(): List<Car> = listOf(
            Car(make = "Jeep", model = "Wrangler", year = 2011, category = Category.JEEP),
            Car(make = "Jeep", model = "Comanche", year = 1990, category = Category.JEEP),
            Car(make = "Dodge", model = "Avenger", year = 2010, category = Category.SEDAN),
            Car(make = "Buick", model = "Cascada", year = 2016, category = Category.CONVERTIBLE),
            Car(make = "Ford", model = "Focus", year = 2012, category = Category.SEDAN),
            Car(make = "Chevrolet", model = "Geo Metro", year = 1992, category = Category.CONVERTIBLE))

    data class Person(val cars: List<Car>)

    fun getModelsAfter2000(cars: List<Car>): List<String> = cars
        .filter { it.year > 2000 }
        .sortedBy { it.year }
        .map { it.model }

    fun getGroupingOfCarsByCategory(cars: List<Car>): Map<Category, List<Car>> = cars.groupBy { it.category }

    fun getSedanCarsOwnedSortedByDate(persons: List<Person>): List<Car> =
        persons.flatMap { p -> p.cars }
            .filter { c -> c.category == Category.SEDAN }
            .sortedBy { c -> c.year }

    @JvmStatic
    fun main(args: Array<String>) {
        val cars: List<Car> = createCars()
        log.info(getModelsAfter2000(cars).joinToString("\n"))
        log.info(getGroupingOfCarsByCategory(cars).toString())
        val john = Person(cars)
        log.info(getSedanCarsOwnedSortedByDate(listOf(john)).joinToString("\n"))
    }
}