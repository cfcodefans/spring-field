package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/caching
 */
object Caching {
    private val log: Logger = LoggerFactory.getLogger(Caching::class.java)

    data class UserAccount(var userId: String,
                           var userName: String,
                           var additionalInfo: String)

    interface IDbManager {
        fun connect()
        fun disconnect()
        fun readFromDb(userId: String): UserAccount?
        fun writeToDb(userAcc: UserAccount): UserAccount
        fun updateDb(userAcc: UserAccount): UserAccount
        fun upsertDb(userAcc: UserAccount): UserAccount
    }

    open class VirtualDb : IDbManager {
        private var db: MutableMap<String, UserAccount>? = null
        override fun connect() {
            db = HashMap<String, UserAccount>()
        }

        override fun disconnect() {
            db = null
        }

        override fun readFromDb(userId: String): UserAccount? = db!![userId]
        override fun writeToDb(userAcc: UserAccount): UserAccount = userAcc.apply {
            log.info("DB is writing\t${userAcc.userId}")
            db!![userAcc.userId] = userAcc
        }

        override fun updateDb(userAcc: UserAccount): UserAccount = writeToDb(userAcc)
        override fun upsertDb(userAcc: UserAccount): UserAccount = updateDb(userAcc)

        override fun toString(): String = db!!
            .values
            .joinToString(separator = "\n",
                    prefix = "\n--Database CONTENT--\n",
                    postfix = "\n----")
    }

    open class LruCache(private var capacity: Int) {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(LruCache::class.java)

            data class Node(val userId: String,
                            var userAcc: UserAccount?) {
                var previous: Node? = null
                var next: Node? = null
            }
        }

        private val cache: MutableMap<String, Node> = HashMap()
        private var head: Node? = null
        private var end: Node? = null

        /**
         * Remove node from linked list.
         *
         * @param node [Node]
         */
        fun remove(node: Node) {
            if (node.previous == null) head = node.next
            else node.previous!!.next = node.next

            if (node.next == null) end = node.previous
            else node.next?.previous = node.previous
        }

        fun setHead(node: Node): LruCache = apply {
            node.next = head
            node.previous = null
            head?.previous = node
            head = node
            if (end == null) end = head
        }


        operator fun get(userId: String): UserAccount? = if (cache.containsKey(userId)) {
            val node: Node = cache[userId]!!
            remove(node)
            setHead(node)
            node.userAcc!!
        } else null

        operator fun set(userId: String, userAcc: UserAccount): UserAccount? {
            if (cache.containsKey(userId)) {
                val old: Node = cache[userId]!!
                val oldAcc: UserAccount = old.userAcc!!
                old.userAcc = userAcc
                remove(old)
                setHead(old)
                return oldAcc
            }

            val newNode: Node = Node(userId, userAcc)
            if (cache.size >= capacity) {
                log.info("# Cache is FULL! Removing ${end!!.userId}")
                cache.remove(end!!.userId)
                remove(end!!)
                setHead(newNode)
            } else {
                setHead(newNode)
            }
            cache[userId] = newNode
            return null
        }

        fun contains(userId: String): Boolean = cache.containsKey(userId)

        fun invalidate(userId: String): UserAccount? = cache
            .remove(userId)
            ?.also {
                log.info("# $userId has been updated! Removing older version from cache...")
                remove(it)
            }?.userAcc

        fun isFull(): Boolean = cache.size >= capacity

        fun getLeastUsed(): UserAccount? = end?.userAcc

        fun clear() {
            head = null
            end = null
            cache.clear()
        }

        fun toList(): List<UserAccount> = head
            ?.let { h ->
                generateSequence<Node>(h)
                { n: Node -> n.next }
                    .map { n -> n.userAcc!! }
                    .toList()
            } ?: emptyList<UserAccount>()

        fun setCapacity(newCapacity: Int): LruCache = apply {
            if (capacity > newCapacity) clear()
            else this.capacity = newCapacity
        }
    }

    enum class CachingPolicy(val policy: String) {
        /**
         * Through.
         */
        THROUGH("through"),

        /**
         * AROUND.
         */
        AROUND("around"),

        /**
         * BEHIND.
         */
        BEHIND("behind"),

        /**
         * ASIDE.
         */
        ASIDE("aside");
    }

    private const val CAPACITY: Int = 3

    open class CacheStore(private val dbManager: IDbManager) {
        companion object {
            private val log: Logger = LoggerFactory.getLogger(CacheStore::class.java)
        }

        private lateinit var cache: LruCache

        fun initCapacity(capacity: Int): CacheStore = apply {
            cache = LruCache(capacity)
        }

        init {
            initCapacity(CAPACITY)
        }

        /**
         * Get user account using read-through cache.
         * 1. return the value if cache has it
         * 2. read from db, update cache, return value
         */
        fun readThrough(userId: String): UserAccount? {
            if (cache.contains(userId)) {
                log.info("# ${userId} Found in cache!")
                return cache[userId]
            }
            log.info("# ${userId} Not found in cache! Go to DB!!")
            return dbManager
                .readFromDb(userId)
                ?.also { cache[userId] = it }
        }

        /**
         * Write user Account using write-through cache.
         * update db firstly, update cache secondly
         */
        fun writeThrough(userAcc: UserAccount): UserAccount? {
            if (cache.contains(userAcc.userId))
                dbManager.updateDb(userAcc)
            else
                dbManager.writeToDb(userAcc)
            return cache.set(userAcc.userId, userAcc)
        }

        fun writeAround(userAcc: UserAccount): UserAccount? =
            if (cache.contains(userAcc.userId)) {
                dbManager.upsertDb(userAcc)
                cache.invalidate(userAcc.userId)
            } else {
                dbManager.writeToDb(userAcc)
                null
            }

        /**
         * Get user account using read-through cache with write-back policy
         */
        fun readThroughWithWriteBackPolicy(userId: String): UserAccount? {
            if (cache.contains(userId)) {
                log.info("# $userId Found in cache!")
                return cache[userId]
            }
            log.info("# $userId is not found in cache!")
            val userAcc: UserAccount = dbManager.readFromDb(userId) ?: return null

            if (cache.isFull()) {
                log.info(" Cache is FULL! Writing LRU data to DB...")
                dbManager.updateDb(cache.getLeastUsed()!!)
            }
            cache[userId] = userAcc
            return userAcc
        }

        /**
         * write db firstly in absence of the key in the cache,
         * and cache secondly
         * the cache is flushed to db when program exits
         * @return the old version of UserAccount
         */
        fun writeBehind(userAcc: UserAccount): UserAccount? {
            if (cache.isFull() && cache.contains(userAcc.userId).not()) {
                log.info("# Cache is FULL! Writing LRU data to DB...")
                dbManager.updateDb(cache.getLeastUsed()!!)
            }

            return cache.set(userAcc.userId, userAcc)
        }

        fun clearCache(): CacheStore = apply { cache.clear() }

        fun flushCache(): CacheStore = apply {
            log.info("# flush cache...")
            cache.toList()
                .forEach(dbManager::upsertDb)
            dbManager.disconnect()
        }

        override fun toString(): String = cache
            .toList()
            .joinToString(separator = "\n",
                    prefix = "\n--CACHE CONTENT--\n",
                    postfix = "\n----")

        operator fun get(userId: String): UserAccount? = cache[userId]
        operator fun set(userId: String, userAcc: UserAccount): UserAccount? = cache.set(userId, userAcc)

        fun invalidate(userId: String): UserAccount? = cache.invalidate(userId)
    }

    /**
     * AppManager helps to bridge the gap in communication between the main class
     * and the application's back-end. DB connection is initialized through this
     * class. The chosen  caching strategy/policy is also initialized here.
     * Before the cache can be used, the size of the  cache has to be set.
     * Depending on the chosen caching policy, AppManager will call the
     * appropriate function in the CacheStore class.
     */
    open class AppManager(private val dbManager: IDbManager) {
        private val cacheStore: CacheStore = CacheStore(dbManager)
        private lateinit var cachingPolicy: CachingPolicy

        /**
         * Developer/Tester is able to choose whether the application should use
         * MongoDB as its underlying data storage or a simple Java data structure
         * to (temporarily) store the data/objects during runtime.
         */
        fun initDb() = dbManager.connect()

        /**
         * Initialize caching policy.
         *
         * @param policy is a [CachingPolicy]
         */
        fun initCachingPolicy(policy: CachingPolicy) {
            cachingPolicy = policy
            if (cachingPolicy == CachingPolicy.BEHIND) {
                Runtime.getRuntime().addShutdownHook(Thread { cacheStore.flushCache() })
            }
            cacheStore.clearCache()
        }

        /**
         * Find user account.
         *
         * @param userId String
         * @return [UserAccount]
         */
        fun find(userId: String): UserAccount? {
            log.info("Trying to find $userId in cache")
            return when (cachingPolicy) {
                CachingPolicy.THROUGH,
                CachingPolicy.AROUND -> cacheStore.readThrough(userId)
                CachingPolicy.BEHIND -> cacheStore.readThroughWithWriteBackPolicy(userId!!)
                CachingPolicy.ASIDE -> findAside(userId)
                else -> null
            }
        }

        /**
         * Save user account.
         *
         * @param userAcc [UserAccount]
         */
        fun save(userAcc: UserAccount): UserAccount? {
            log.info("Save record! ${userAcc.userId}")
            return when (cachingPolicy) {
                CachingPolicy.THROUGH -> cacheStore.writeThrough(userAcc)
                CachingPolicy.AROUND -> cacheStore.writeAround(userAcc)
                CachingPolicy.BEHIND -> cacheStore.writeBehind(userAcc)
                CachingPolicy.ASIDE -> saveAside(userAcc)
            }
        }

        private fun saveAside(userAccount: UserAccount): UserAccount? = run {
            dbManager.updateDb(userAccount)
            cacheStore.invalidate(userAccount.userId)
        }

        /**
         * Cache-Aside find user account helper.
         *
         * @param userId String
         * @return [UserAccount]
         */
        private fun findAside(userId: String): UserAccount? = cacheStore[userId]
            ?: (dbManager.readFromDb(userId)
                ?.also { cacheStore[userId] = it })

        override fun toString(): String = """
            $cacheStore
            $dbManager
            """.trimIndent()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val appManager: AppManager = AppManager(VirtualDb()).also { it.initDb() }

        run {
            log.info("# CachingPolicy.THROUGH")
            appManager.initCachingPolicy(CachingPolicy.THROUGH)
            val userAcc: UserAccount = UserAccount(userId = "001", userName = "John", additionalInfo = "He is a boy.")
            appManager.save(userAcc)
            log.info(appManager.toString())
            appManager.find("001")
            appManager.find("001")
        }

        log.info("\n".repeat(5))

        run {
            log.info("# CachingPolicy.AROUND")
            appManager.initCachingPolicy(CachingPolicy.AROUND)
            var userAcc: UserAccount = UserAccount(userId = "002", userName = "jane", additionalInfo = "She is a girl.")

            appManager.save(userAcc)
            log.info(appManager.toString())
            appManager.find("002")
            log.info(appManager.toString())
            userAcc = appManager.find("002")!!
            userAcc.userName = "Jane G."
            appManager.save(userAcc)
            log.info(appManager.toString())
            appManager.find("002")
            log.info(appManager.toString())
            appManager.find("002")
            log.info(appManager.toString())
        }

        log.info("\n".repeat(5))

        run {
            log.info("# CachingPolicy.BEHIND")
            appManager.initCachingPolicy(CachingPolicy.BEHIND)

            var userAcc3: UserAccount = UserAccount(userId = "003", userName = "Adam", additionalInfo = "He likes food.")
            var userAcc4: UserAccount = UserAccount(userId = "004", userName = "Rita", additionalInfo = "She hates cats.")
            var userAcc5: UserAccount = UserAccount(userId = "005", userName = "Isaac", additionalInfo = "He is allergic to mustard.")

            appManager.save(userAcc3)
            appManager.save(userAcc4)
            appManager.save(userAcc5)

            log.info(appManager.toString())

            appManager.find("003")
            log.info(appManager.toString())

            val userAcc6: UserAccount = UserAccount("006",
                    "Yasha",
                    "She is an only child.")
            appManager.save(userAcc6)
            log.info(appManager.toString())
            appManager.find("004")
            log.info(appManager.toString())
        }
    }
}