package cfcodefans.study.spring_field.patterns.cqrs

open class UserRepo(val store: MutableMap<String, User> = HashMap()) {
    open fun addUser(id: String, user: User): UserRepo = apply {
        store[id] = user
    }

    open fun addUser(user: User): UserRepo = addUser(user.userId, user)

    open fun getUser(id: String): User? = store[id]

    open fun deleteUser(id: String): User? = store.remove(id)
}

open class UserService(val userRepo: UserRepo) {

    open fun createUser(userId: String,
                        firstName: String,
                        lastName: String): User = User(userId, firstName, lastName).also { userRepo.addUser(userId, it) }

    open fun updateUser(userId: String,
                        contacts: MutableSet<Contact>,
                        addresses: MutableSet<Address>): User {
        val user: User? = userRepo.getUser(userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }

        return user.let {
            it.contacts = contacts
            it.addresses = addresses
            userRepo.addUser(it)
            it
        }
    }

    open fun getContactByType(userId: String, contactType: String): Set<Contact> {
        val user: User? = userRepo.getUser(userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }
        return user.contacts.filter { it.type == contactType }.toSet()
    }

    open fun getAddrByRegion(userId: String, state: String): Set<Address> {
        val user: User? = userRepo.getUser(userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }
        return user.addresses.filter { it.state == state }.toSet()
    }

}