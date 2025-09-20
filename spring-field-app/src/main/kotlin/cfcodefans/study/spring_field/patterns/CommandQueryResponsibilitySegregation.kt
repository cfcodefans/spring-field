package cfcodefans.study.spring_field.patterns

import jakarta.persistence.*
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.service.ServiceRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/cqrs
 */
object CommandQueryResponsibilitySegregation {

    private val log: Logger = LoggerFactory.getLogger(CommandQueryResponsibilitySegregation::class.java)

    const val E_EVANS: String = "eEvans"
    const val J_BLOCH: String = "jBloch"
    const val M_FOWLER: String = "mFowler"
    const val USER_NAME: String = "username"

    @Entity(name = "Author")
    @Table(name = "author")
    data class Author(@Id
                      @GeneratedValue(strategy = GenerationType.IDENTITY)
                      var id: Long? = null,
                      var username: String = "",
                      var name: String = "",
                      var email: String = "") {
        override fun equals(other: Any?): Boolean = other is Author
                && ((other.id != null && this.id != null && other.id == this.id)
                || (username == other.username && username.isNotBlank()))

        override fun hashCode(): Int = Objects.hash(Author::class,
                                                    if (id != null) id.hashCode() else username)
    }

    @Entity(name = "Book")
    @Table(name = "book")
    data class Book(@Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    var id: Long? = null,
                    var title: String = "",
                    var price: BigDecimal? = null,
                    @ManyToOne
                    var author: Author? = null) {

        override fun equals(other: Any?): Boolean = other is Book
                && ((other.id != null && other.id == this.id)
                || (title == other.title && title.isNotBlank()))

        override fun hashCode(): Int = Objects.hash(Book::class, if (id != null) id.hashCode() else title)
    }

    data class AuthorDTO(val name: String, val email: String, val username: String)

    fun Author.toDTO(): AuthorDTO = AuthorDTO(name = this.name, email = this.email, username = this.username)

    data class BookDTO(val title: String, val price: BigDecimal?)

    fun Book.toDTO(): BookDTO = BookDTO(title = this.title, price = this.price)

    interface IQueryService {
        fun getAuthorByUsername(username: String): AuthorDTO?
        fun getBook(title: String): BookDTO?
        fun getAuthorBooks(username: String): List<BookDTO>
        fun getAuthBooksCount(username: String): Int
        fun getAuthorsCount(): Int
    }

    open class QueryService : IQueryService {
        private val sf: SessionFactory = SESSION_FACTORY
        override fun getAuthorByUsername(username: String): AuthorDTO? = sf
            .openSession()
            .use { s ->
                s.createQuery("select a from Author a where a.username=:username", Author::class.java)
                    .setParameter(USER_NAME, username)
                    .uniqueResult()
                    ?.toDTO()
            }

        override fun getBook(title: String): BookDTO? = sf
            .openSession()
            .use { s ->
                s.createQuery("select b from Book b where b.title=:title", Book::class.java)
                    .setParameter("title", title)
                    .uniqueResult()
                    ?.toDTO()
            }

        override fun getAuthorBooks(username: String): List<BookDTO> = sf
            .openSession()
            .use { s ->
                s.createQuery("""
                select b from Author a, Book b 
                where b.author.id = a.id and a.username = :username""".trimIndent(),
                              Book::class.java)
                    .setParameter(USER_NAME, username)
                    .resultList
                    .map { it.toDTO() }
            }

        override fun getAuthorsCount(): Int = sf
            .openSession()
            .use { s ->
                (s.createNativeQuery("select count(id) from author")
                    .uniqueResult() as Long)
                    .toInt()
            }

        override fun getAuthBooksCount(username: String): Int = sf
            .openSession()
            .use { s ->
                (s.createNativeQuery("""
                    select count(b.title) 
                    from book b, author a 
                    where b.author_id=a.id and a.username=:username
                    """.trimIndent())
                    .setParameter(USER_NAME, username)
                    .uniqueResult() as Long)
                    .toInt()
            }
    }

    interface ICommandService {
        fun authorCreated(username: String, name: String, email: String)
        fun bookAddedToAuthor(title: String, price: BigDecimal, username: String)
        fun authorNameUpdated(username: String, name: String)
        fun authorUsernameUpdated(username: String, name: String)
        fun authorEmailUpdated(username: String, email: String)
        fun bookTitleUpdated(oldTitle: String, newTitle: String)
        fun bookPriceUpdated(title: String, price: BigDecimal?)
    }

    open class CommandService : ICommandService {
        private val sf: SessionFactory = SESSION_FACTORY

        private fun getAuthorByUsername(username: String): Author = sf
            .openSession()
            .use { s ->
                s.createQuery("select a from Author a where a.username=:username", Author::class.java)
                    .setParameter(USER_NAME, username)
                    .uniqueResult()
            } ?: throw NullPointerException("Author $username is not found")

        private fun getBookByTitle(title: String): Book = sf
            .openSession()
            .use { s ->
                s.createQuery("select b from Book b where b.title=:title", Book::class.java)
                    .setParameter("title", title)
                    .uniqueResult()
            } ?: throw NullPointerException("Book $title is not found")

        override fun authorCreated(username: String, name: String, email: String) {
            tx { s -> s.persist(Author(username = username, name = name, email = email)) }
        }

        override fun bookAddedToAuthor(title: String, price: BigDecimal, username: String) {
            tx { s -> s.persist(Book(title = title, price = price, author = getAuthorByUsername(username))) }
        }

        override fun authorNameUpdated(username: String, name: String) {
            tx { s ->
                getAuthorByUsername(username)
                    .apply { this.name = name }
                    .let { s.merge(it) }
            }
        }

        override fun authorUsernameUpdated(oldUsername: String, newUsername: String) {
            tx { s ->
                getAuthorByUsername(oldUsername)
                    .apply { this.username = newUsername }
                    .let { s.merge(it) }
            }
        }

        override fun authorEmailUpdated(username: String, email: String) {
            tx { s ->
                getAuthorByUsername(username)
                    .apply { this.email = email }
                    .let { s.merge(it) }
            }
        }

        override fun bookTitleUpdated(oldTitle: String, newTitle: String) {
            tx { s ->
                getBookByTitle(oldTitle)
                    .apply { title = newTitle }
                    .let { s.merge(it) }
            }
        }

        override fun bookPriceUpdated(title: String, price: BigDecimal?) {
            tx { s ->
                getBookByTitle(title)
                    .apply { this.price = price }
                    .let { s.merge(it) }
            }
        }
    }

    fun <R> tx(oper: (Session) -> R): R = SESSION_FACTORY
        .openSession()
        .use { s -> return@tx oper(s) }

    val SESSION_FACTORY: SessionFactory = StandardServiceRegistryBuilder()
        .let {
            val cfgs: Map<String, String> = mapOf("hibernate.connection.driver_class" to "org.hsqldb.jdbc.JDBCDriver",
                                                  "hibernate.connection.url" to "jdbc:hsqldb:mem:test",
                                                  "hibernate.connection.username" to "sa",
                                                  "hibernate.connection.password" to "",
                                                  "hibernate.dialect" to "org.hibernate.dialect.HSQLDialect",
                                                  "hibernate.show_sql" to "true",
                                                  "hibernate.format_sql" to "true",
                                                  "hibernate.hbm2ddl.auto" to "create-drop")
            val config: Configuration = Configuration()
                .apply {
                    cfgs.forEach { it -> this.setProperty(it.key, it.value) }
                    addAnnotatedClass(Author::class.java)
                    addAnnotatedClass(Book::class.java)
                }
            val registry: ServiceRegistry = it.applySettings(config.properties).build()
            try {
                return@let config.buildSessionFactory(registry)
            } catch (ex: Exception) {
                StandardServiceRegistryBuilder.destroy(registry)
                log.error("Initiating SessionFactory failed with $cfgs", ex)
                throw ExceptionInInitializerError(ex)
            }
        }


    /**
     * CQRS: Command Query Responsibility Segregation. A pattern used to separate query services from
     * commands or writes services. The pattern is very simple, but it has many consequences.
     * For example, it can be used to tackle down a complex domain, or to use other architectures that were
     * hard to implement with the classical way.
     *
     * <p> This implementation is an example of managing books and authors in a library. The persistence
     * of books and authors is done according to the CQRS architecture. A command side that deals with a
     * data model to persist(insert, update, delete) objects to a database. And a query side that uses
     * native queries to get data from the database and return objects as DTOs (Data transfer objects)
     */
    @JvmStatic
    fun main(args: Array<String>) {
//        SESSION_FACTORY.openSession().use { sess ->
//            val books: List<Book> = sess.createQuery("select b from Book b", Book::class.java).resultList
//            log.info(books.joinToString("\n"))
//        }

        val cmds: ICommandService = CommandService()

        //create Authors and books using CommandService
        cmds.authorCreated(E_EVANS, "Eric Evans", "evans@email.com")
        cmds.authorCreated(J_BLOCH, "Joshua Bloch", "jBloch@email.com")
        cmds.authorCreated(M_FOWLER, "Martin Fowler", "mFowler@email.com")

        cmds.bookAddedToAuthor("Domain-Driven Design", 60.08.toBigDecimal(), E_EVANS)
        cmds.bookAddedToAuthor("Effective Java", 40.54.toBigDecimal(), J_BLOCH)
        cmds.bookAddedToAuthor("Java Puzzlers", 39.99.toBigDecimal(), J_BLOCH)
        cmds.bookAddedToAuthor("Java Concurrency in Practice", 29.40.toBigDecimal(), J_BLOCH)
        cmds.bookAddedToAuthor("Patterns of Enterprise Application Architecture", 54.01.toBigDecimal(), M_FOWLER)
        cmds.bookAddedToAuthor("Domain Specific Languages", 48.89.toBigDecimal(), M_FOWLER)
        cmds.authorNameUpdated(E_EVANS, "Eric J. Evans")

        val qs: IQueryService = QueryService()
        // Query the database using QueryService
        val nullAuthor: AuthorDTO? = qs.getAuthorByUsername("username")
        val evans: AuthorDTO? = qs.getAuthorByUsername(E_EVANS)
        val blochBooksCount: Int = qs.getAuthBooksCount(J_BLOCH)
        val authorCount: Int = qs.getAuthorsCount()
        val dddBook: BookDTO? = qs.getBook("Domain-Driven Design")
        val blochBooks: List<BookDTO> = qs.getAuthorBooks(J_BLOCH)

        log.info("Author username : {}", nullAuthor)
        log.info("Author evans : {}", evans)
        log.info("jBloch number of books : {}", blochBooksCount)
        log.info("Number of authors : {}", authorCount)
        log.info("DDD book : {}", dddBook)
        log.info("jBloch books : {}", blochBooks)

        SESSION_FACTORY.close()
    }
}