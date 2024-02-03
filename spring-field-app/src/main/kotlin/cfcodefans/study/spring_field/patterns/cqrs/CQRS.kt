package cfcodefans.study.spring_field.patterns.cqrs

open class UserWriteRepo(val store: MutableMap<String, User> = HashMap()) {
    open fun addUser(id: String, user: User): UserWriteRepo = apply {
        store[id] = user
    }

    open fun addUser(user: User): UserWriteRepo = addUser(user.userId, user)
    open fun getUser(id: String): User? = store[id]
}

open class UserReadRepo(val userAddresses: MutableMap<String, UserAddress> = HashMap(),
                        val userContacts: MutableMap<String, UserContact> = HashMap()) {
    open fun addUserAddr(userId: String, addr: UserAddress): UserReadRepo = apply {
        userAddresses[userId] = addr
    }

    open fun getUserAddr(userId: String): UserAddress? = userAddresses[userId]
    open fun addUserContact(userId: String, contact: UserContact): UserReadRepo = apply {
        userContacts[userId] = contact
    }

    open fun getUserContact(userId: String): UserContact? = userContacts[userId]
}

data class CreateUserCmd(val userId: String, val firstName: String, val lastName: String)

data class UpdateUserCmd(val userId: String, val addresses: MutableSet<Address> = HashSet(), val contacts: MutableSet<Contact> = HashSet())

data class AddrByRegionQuery(val userId: String, val state: String)
data class ContactByTypeQuery(val userId: String, val contactType: String)

open class UserAggregate(val writeRepo: UserWriteRepo = UserWriteRepo()) {

    open fun handleCreateUserCmd(cmd: CreateUserCmd): User =
        User(userId = cmd.userId,
                firstName = cmd.firstName,
                lastName = cmd.lastName)
            .also { writeRepo.addUser(it) }

    open fun handleUpdateUserCmd(cmd: UpdateUserCmd): User? =
        writeRepo.getUser(cmd.userId)
            ?.let { user ->
                user.addresses = cmd.addresses
                user.contacts = cmd.contacts
                writeRepo.addUser(user)
                user
            }
}

open class UserProjection(val readRepo: UserReadRepo = UserReadRepo()) {
    open fun handle(query: ContactByTypeQuery): Set<Contact> {
        val userContact: UserContact? = readRepo.getUserContact(userId = query.userId)
        requireNotNull(userContact) { "User: ${query.userId} doesn't exist." }
        return userContact.contactByType[query.contactType] ?: emptySet()
    }

    open fun handle(query: AddrByRegionQuery): Set<Address> {
        val userAddr: UserAddress? = readRepo.getUserAddr(userId = query.userId)
        requireNotNull(userAddr) { "User: ${query.userId} doesn't exist." }
        return userAddr.addressByRegion[query.state] ?: emptySet()
    }
}

open class UserProjector(val readRepo: UserReadRepo = UserReadRepo()) {
    open fun project(user: User): User {
        val userContact: UserContact = readRepo.getUserContact(user.userId) ?: UserContact()

        userContact.contactByType = user.contacts
            .groupBy { it.type }
            .entries.associate { en -> en.key to en.value.toMutableSet() }
            .let { HashMap(it) }

        readRepo.addUserContact(userId = user.userId, userContact)

        val userAddr: UserAddress = readRepo.getUserAddr(user.userId) ?: UserAddress()

        userAddr.addressByRegion = user.addresses
            .groupBy { it.state }
            .entries.associate { en -> en.key to en.value.toMutableSet() }
            .let { HashMap(it) }

        readRepo.addUserAddr(userId = user.userId, userAddr)

        return user
    }
}