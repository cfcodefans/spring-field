package cfcodefans.study.spring_field.patterns.cqrs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

open class EventSourceTests {
    lateinit var evRepo: EventStore
    lateinit var userEvService: UserEvService

    @BeforeEach
    open fun setUp() {
        evRepo = EventStore()
        userEvService = UserEvService(evRepo)
    }

    @Test
    open fun testEv() {
        val userId: String = UUID.randomUUID().toString()

        userEvService.createUser(userId, "Tom", "Sawyer")
        userEvService.updateUser(userId,
                mutableSetOf(Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("EMAIL", "tom.sawyer@rediff.com"),
                        Contact("PHONE", "700-000-0001")),
                mutableSetOf(Address("New York", "NY", "10001"),
                        Address("Los Angeles", "CA", "90001"),
                        Address("Housten", "TX", "77001")))

        userEvService.updateUser(userId,
                mutableSetOf(Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("PHONE", "700-000-0001")),
                mutableSetOf(Address("New York", "NY", "10001"),
                        Address("Housten", "TX", "77001")))

        assertEquals(setOf(Contact("EMAIL", "tom.sawyer@gmail.com")),
                userEvService.getContactByType(userId, "EMAIL"))
        assertEquals(setOf(Address("New York", "NY", "10001")),
                userEvService.getAddrByRegion(userId, "NY"))
    }
}