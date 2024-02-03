package cfcodefans.study.spring_field.patterns.cqrs

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class Event(val id: UUID = UUID.randomUUID(),
                     val created: Date = Date()) {
    override fun toString(): String = "id: $id,\tcreated: $created"
}

data class UserCreateEv(val userId: String,
                        val firstName: String,
                        val lastName: String) : Event()

data class UserAddrAddedEv(val city: String,
                           val state: String,
                           val postCode: String) : Event()

data class UserAddrRemovedEv(val city: String,
                             val state: String,
                             val postCode: String) : Event()

data class UserContactAddedEv(val contactType: String,
                              val contactDetails: String) : Event()

data class UserContactRemovedEv(val contactType: String,
                                val contactDetails: String) : Event()


open class EventStore(val store: MutableMap<String, MutableList<Event>> = HashMap()) {
    open fun addEvent(id: String, ev: Event): EventStore = apply {
        store.computeIfAbsent(id) { ArrayList() }
            .add(ev)
    }

    open fun getEvents(id: String): List<Event> = store[id] ?: emptyList()

    companion object {
        open fun recreateUserState(store: EventStore, userId: String): User? {
            var user: User? = null

            for (ev: Event in store.getEvents(userId)) {
                when (ev) {
                    is UserCreateEv -> user = User(userId = ev.userId, firstName = ev.firstName, lastName = ev.lastName)
                    is UserAddrAddedEv -> user?.addresses?.add(Address(city = ev.city, state = ev.state, postcode = ev.postCode))
                    is UserAddrRemovedEv -> user?.addresses?.remove(Address(city = ev.city, state = ev.state, postcode = ev.postCode))
                    is UserContactAddedEv -> user?.contacts?.add(Contact(type = ev.contactType, detail = ev.contactDetails))
                    is UserContactRemovedEv -> user?.contacts?.remove(Contact(type = ev.contactType, detail = ev.contactDetails))
                }
            }

            return user
        }
    }
}

open class UserEvService(val evRepo: EventStore) {
    open fun createUser(userId: String,
                        firstName: String,
                        lastName: String) {
        evRepo.addEvent(userId, UserCreateEv(userId, firstName, lastName))
    }

    open fun updateUser(userId: String,
                        contacts: Set<Contact>,
                        addresses: Set<Address>): List<Event> {
        val user: User? = EventStore.recreateUserState(evRepo, userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }

        val evList: List<Event> = (user.contacts
            .subtract(contacts)
            .map { c -> UserContactRemovedEv(c.type, c.detail) }
                ) + (contacts.subtract(user.contacts)
            .map { c -> UserContactAddedEv(c.type, c.detail) }
                ) + (user.addresses
            .subtract(addresses)
            .map { a -> UserAddrRemovedEv(a.city, a.state, a.postcode) }
                ) + (addresses.subtract(user.addresses)
            .map { a -> UserAddrAddedEv(a.city, a.state, a.postcode) })

        evList.forEach { ev -> evRepo.addEvent(userId, ev) }

        return evList
    }

    open fun getContactByType(userId: String, contactType: String): Set<Contact> {
        val user: User? = EventStore.recreateUserState(evRepo, userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }

        return user.contacts.filter { c -> c.type == contactType }.toSet()
    }

    open fun getAddrByRegion(userId: String, state: String): Set<Address> {
        val user: User? = EventStore.recreateUserState(evRepo, userId)
        requireNotNull(user) { "user: ${userId} doesn't exist." }

        return user.addresses.filter { a -> a.state == state }.toSet()
    }
}

