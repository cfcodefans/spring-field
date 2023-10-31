package cfcodefans.study.spring_field.patterns.cqrs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals


open class CQRSTests {
    lateinit var writeRepo: UserWriteRepo
    lateinit var readRepo: UserReadRepo
    lateinit var userProjector: UserProjector
    lateinit var userAggregate: UserAggregate
    lateinit var userProjection: UserProjection

    @BeforeEach
    open fun setUp() {
        writeRepo = UserWriteRepo()
        readRepo = UserReadRepo()
        userProjector = UserProjector(readRepo)
        userAggregate = UserAggregate(writeRepo)
        userProjection = UserProjection(readRepo)
    }

    @Test
    open fun givenCQRSApplication_whenCommandRun_thenQueryShouldReturnResult() {
        val userId: String = UUID.randomUUID().toString()

        val createUserCmd: CreateUserCmd = CreateUserCmd(userId, "Tom", "Sawyer")
        var user: User? = userAggregate.handleCreateUserCmd(createUserCmd)
        userProjector.project(user!!)

        val updateUserCmd1: UpdateUserCmd = UpdateUserCmd(userId = user.userId,
                contacts = mutableSetOf(
                        Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("EMAIL", "tom.sawyer@rediff.com")))
        user = userAggregate.handleUpdateUserCmd(updateUserCmd1)
        userProjector.project(user!!)

        val updateUserCmd2: UpdateUserCmd = UpdateUserCmd(userId = user.userId,
                addresses = mutableSetOf(
                        Address("New York", "NY", "10001"),
                        Address("Housten", "TX", "77001")),
                contacts = mutableSetOf(
                        Contact("EMAIL", "tom.sawyer@gmail.com"),
                        Contact("PHONE", "700-000-0001")
                ))
        user = userAggregate.handleUpdateUserCmd(updateUserCmd2)
        userProjector.project(user!!)

        val contactByTypeQuery: ContactByTypeQuery = ContactByTypeQuery(userId, "EMAIL")
        assertEquals(setOf(Contact("EMAIL", "tom.sawyer@gmail.com")),
                userProjection.handle(contactByTypeQuery))
        val addrByRegionQuery: AddrByRegionQuery = AddrByRegionQuery(userId, "NY")
        assertEquals(setOf(Address("New York", "NY", "10001")),
                userProjection.handle(addrByRegionQuery))
    }
}