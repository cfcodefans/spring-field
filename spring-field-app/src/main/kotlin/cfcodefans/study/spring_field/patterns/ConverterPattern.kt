package cfcodefans.study.spring_field.patterns

import cfcodefans.study.spring_field.patterns.ConverterPattern.UserDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Function


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/converter
 * Intent:
 *      The purpose of the Converter pattern is to provide a generic, common way of
 *      bidirectional conversion between corresponding types, allowing a clean implementation in
 *      which the types do not need to be aware of each other.
 *      Moreover, the Converter pattern introduces bidirectional collection mapping,
 *      reducing a boilerplate code to minimum
 */
object ConverterPattern {
    private val log: Logger = LoggerFactory.getLogger(Converter::class.java)

    data class User(val firstName: String,
                    val lastName: String,
                    val active: Boolean,
                    val userId: String)

    data class UserDto(val firstName: String,
                       val lastName: String,
                       val active: Boolean,
                       val userId: String)

    open class Converter<T, U>(private val fromDto: (T) -> U,
                               private val fromEntity: (U) -> T) {
        fun convertFromDto(dto: T): U = fromDto(dto)
        fun convertFromEntity(entity: U): T = fromEntity(entity)

        fun convertFromDtos(dtos: Iterable<T>): List<U> = dtos.map(fromDto)
        fun convertFromEntities(entities: Iterable<U>): List<T> = entities.map(fromEntity)
    }

    fun convertToDto(user: User): UserDto = UserDto(firstName = user.firstName,
            lastName = user.lastName,
            active = user.active,
            userId = user.userId)

    fun convertToEntity(userDto: UserDto): User = User(firstName = userDto.firstName,
            lastName = userDto.lastName,
            active = userDto.active,
            userId = userDto.userId)

    @JvmStatic
    fun main(args: Array<String>) {
        val cvt: Converter<UserDto, User> = Converter<UserDto, User>(
                fromDto = ConverterPattern::convertToEntity,
                fromEntity = ConverterPattern::convertToDto)

        val ud: UserDto = UserDto("John", "Doe", true, "whatever[at]wherever.com")
        val u: User = cvt.convertFromDto(ud)
        log.info("Entity converted from DTO: $ud")

        val users: List<User> = listOf(
                User("Camile", "Tough", false, "124sad"),
                User("Marti", "Luther", true, "42309fd"),
                User("Kate", "Smith", true, "if0243"))
        log.info("Domain entities:")
        users.map(User::toString).forEach(log::info)

        log.info("DTO entities converted from domain:")
        val dtoEntities: List<UserDto> = cvt.convertFromEntities(users)
        dtoEntities.map(UserDto::toString).forEach(log::info)
    }
}