package cfcodefans.study.spring_field.patterns.cqrs.escqrs

import cfcodefans.study.spring_field.patterns.cqrs.*

open class UserAggregate(val writeRepo: EventStore) {
    open fun handleCreateUserCmd(cmd: CreateUserCmd): List<Event> = UserCreateEv(userId = cmd.userId,
            firstName = cmd.firstName,
            lastName = cmd.lastName)
        .also { writeRepo.addEvent(cmd.userId, it) }
        .let { listOf(it) }

    open fun handleUpdateUserCmd(cmd: UpdateUserCmd): List<Event> {
        return UserEvService(writeRepo).updateUser(
                userId = cmd.userId,
                contacts = cmd.contacts,
                addresses = cmd.addresses)
    }
}

open class UserProjector(val readRepo: UserReadRepo) {

    fun project(userId: String, evs: List<Event>) {
        for (ev in evs) {
            when (ev) {
                is UserAddrRemovedEv -> apply(userId, ev)
                is UserAddrAddedEv -> apply(userId, ev)
                is UserContactAddedEv -> apply(userId, ev)
                is UserContactRemovedEv -> apply(userId, ev)
            }
        }
    }

    fun apply(userId: String, ev: UserAddrAddedEv) {
        val address: Address = Address(ev.city, ev.state, ev.postCode)
        val userAddress: UserAddress = readRepo.getUserAddr(userId) ?: UserAddress()

        userAddress.addressByRegion
            .computeIfAbsent(address.state) {
                HashSet()
            }.add(address)

        readRepo.addUserAddr(userId, userAddress)
    }

    fun apply(userId: String, ev: UserAddrRemovedEv) {
        val address: Address = Address(ev.city, ev.state, ev.postCode)
        readRepo.getUserAddr(userId)?.let { userAddress ->
            userAddress.addressByRegion[address.state]?.remove(address)
            readRepo.addUserAddr(userId, userAddress)
        }
    }

    fun apply(userId: String, ev: UserContactAddedEv) {
        val contact: Contact = Contact(ev.contactType, ev.contactDetails)
        val userContact: UserContact = readRepo.getUserContact(userId) ?: UserContact()

        userContact.contactByType
            .computeIfAbsent(contact.type) {
                HashSet()
            }.add(contact)

        readRepo.addUserContact(userId, userContact)
    }

    fun apply(userId: String, ev: UserContactRemovedEv) {
        val contact: Contact = Contact(ev.contactType, ev.contactDetails)
        readRepo.getUserContact(userId)?.let { userContact ->
            userContact.contactByType[contact.type]?.remove(contact)
            readRepo.addUserContact(userId, userContact)
        }
    }
}
