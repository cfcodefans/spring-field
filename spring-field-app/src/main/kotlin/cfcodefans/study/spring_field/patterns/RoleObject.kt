package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/role-object
 * Intent:
 *  Adapt an object to different client's needs through transparently attached role objects,
 *  each one representing a role the object has to play in that client's context. The object
 *  manages its role set dynamically. By representing roles as individual objects. different contexts
 *  are kept separate and system configuration is simplified.
 */
object RoleObject {
    private val log: Logger = LoggerFactory.getLogger(RoleObject::class.java)

    abstract class Customer {
        abstract fun addRole(role: Role): Boolean
        abstract fun hasRole(role: Role): Boolean
        abstract fun removeRole(role: Role): Boolean
        abstract fun <T : CustomerRole> getRole(role: Role, expectedRole: Class<T>): T?
    }

    fun newCustomer(vararg roles: Role): Customer = CustomerCore()
        .also { c -> roles.forEach { c.addRole(it) } }

    open class CustomerCore : Customer() {
        private val roles: MutableMap<Role, CustomerRole> = hashMapOf()

        override fun hasRole(role: Role): Boolean = roles.containsKey(role)
        override fun addRole(role: Role): Boolean = kotlin.runCatching {
            roles[role] = role.instance<CustomerRole>()!!
        }.isSuccess

        override fun removeRole(role: Role): Boolean = roles.remove(role) != null

        override fun <T : CustomerRole> getRole(role: Role, expectedRole: Class<T>): T? =
            roles[role].let {
                if (expectedRole.isInstance(it))
                    it
                else
                    null
            } as T?
    }

    abstract class CustomerRole : CustomerCore() {

    }

    open class BorrowerRole(var name: String? = null) : CustomerRole() {
        open fun borrow(): String = "Borrower $name wants to get some money."
    }

    open class InvestorRole(var name: String? = null,
                            var amountToInvest: Long = 0) : CustomerRole() {
        open fun invest(): String = "Investor $name has invested $amountToInvest dollars."
    }

    enum class Role(private val typeCst: Class<out CustomerRole>) {
        Borrower(BorrowerRole::class.java),
        Investor(InvestorRole::class.java);

        fun <T : CustomerRole> instance(): T? = kotlin.runCatching {
            typeCst.getDeclaredConstructor().newInstance()
        }.onFailure { e -> e.printStackTrace() }
            .getOrNull() as T?
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val customer: Customer = newCustomer(Role.Borrower, Role.Investor)

        log.info("the new customer created: $customer")

        if (customer.hasRole(Role.Borrower)) log.info("customer has a borrower role")
        if (customer.hasRole(Role.Investor)) log.info("customer has an investor role")

        customer.getRole(Role.Investor, InvestorRole::class.java)
            ?.apply {
                amountToInvest = 1000
                name = "Billy"
            }

        customer.getRole(Role.Borrower, BorrowerRole::class.java)
            ?.apply {
                name = "Johny"
            }

        customer.getRole(Role.Investor, InvestorRole::class.java)
            ?.apply { log.info(invest()) }

        customer.getRole(Role.Borrower, BorrowerRole::class.java)
            ?.apply { log.info(borrow()) }
    }
}