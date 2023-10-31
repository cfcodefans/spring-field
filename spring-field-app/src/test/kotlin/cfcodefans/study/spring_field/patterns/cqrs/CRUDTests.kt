package cfcodefans.study.spring_field.patterns.cqrs

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

open class CRUDTests {
    @Test
    open fun givenCRUDApplication_whenDataCreated_thenDataCanBeFetched() {

        val service: UserService = UserService(UserRepo())

        val userId: String = UUID.randomUUID().toString()

        service.createUser(userId, "Tom", "Sawyer")
        service.updateUser(userId,
                mutableSetOf(Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("EMAIL", "tom.sawyer@rediff.com"),
                        Contact("PHONE", "700-000-0001")),
                mutableSetOf(Address("New York", "NY", "10001"),
                        Address("Los Angeles", "CA", "90001"),
                        Address("Housten", "TX", "77001")))

        service.updateUser(userId,
                mutableSetOf(Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("PHONE", "700-000-0001")),
                mutableSetOf(Address("New York", "NY", "10001"),
                        Address("Housten", "TX", "77001")))

        assertEquals(setOf(Contact("EMAIL", "tom.sawyer@gmail.com")),
                service.getContactByType(userId, "EMAIL"))
        assertEquals(setOf(Address("New York", "NY", "10001")),
                service.getAddrByRegion(userId, "NY"))
    }
}