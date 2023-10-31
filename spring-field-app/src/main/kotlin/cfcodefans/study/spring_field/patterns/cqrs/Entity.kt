package cfcodefans.study.spring_field.patterns.cqrs

import jakarta.persistence.Entity

//@Entity
data class Address(var city: String,
                   var state: String,
                   var postcode: String)

//@Entity
data class Contact(var type: String, var detail: String)


//@Entity
data class User(var userId: String,
                var firstName: String,
                var lastName: String,
                var contacts: MutableSet<Contact> = HashSet(),
                var addresses: MutableSet<Address> = HashSet())

data class UserAddress(var addressByRegion: MutableMap<String, MutableSet<Address>> = HashMap())

data class UserContact(var contactByType: MutableMap<String, MutableSet<Contact>> = HashMap())

