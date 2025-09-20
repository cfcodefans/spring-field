package cfcodefans.study.spring_field.spring.examples.cache

import cfcodefans.study.spring_field.commons.Jsons
import cfcodefans.study.spring_field.commons.MiscUtils
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.VFS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
//import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
//import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

@Service
@CacheConfig(cacheNames = ["file", "content"])
open class FileCacheService {
    companion object {
        val log: Logger = LoggerFactory.getLogger(FileCacheService::class.java)
    }

    @Autowired
    @Lazy
    open lateinit var self: FileCacheService


    @Cacheable(cacheNames = ["content"])
    open fun getContent(path: String): ByteArray? {
        log.info("getContent($path)")
        return self.getFileObj(path)?.content?.byteArray
    }

    @CacheEvict(cacheNames = ["content"])
    open fun evictContent(path: String): ByteArray? {
        log.info("evictContent($path)")
        return self.getContent(path)?.also { evictFileObj(path) }
    }

    @Cacheable(cacheNames = ["file"])
    open fun getFileObj(path: String): FileObject? {
        log.info("getFileObj($path)")
        return runCatching { VFS.getManager().resolveFile(path) }.getOrNull()
    }

    @CacheEvict(cacheNames = ["file"])
    open fun evictFileObj(path: String): FileObject? {
        log.info("evictFileObj($path)")
        return self.getFileObj(path)?.also { it.close() }
    }

    @EventListener
    open fun onCacheItemEvicted(event: CacheItemEvictedEvent) {
        log.info("onCacheItemEvicted($event)")
        when (event.value) {
            is FileObject -> event.value.close()
            else -> Unit
        }
    }
}

open class CacheItemEvictedEvent(source: Any,
                                 val key: String,
                                 val value: Any?,
                                 val cause: RemovalCause) : ApplicationEvent(source) {
    override fun toString(): String = mapOf<String, Any?>("source" to super.source,
                                                          "key" to key,
                                                          "value" to value.toString(),
                                                          "cause" to cause).let { Jsons.toString(it) }
}

// --- Adapter and Listener Components ---
@Component
open class CaffeineRemovalListenerAdapter(private val publisher: ApplicationEventPublisher) : RemovalListener<Any, Any> {
    override fun onRemoval(key: Any?, value: Any?, cause: RemovalCause) {
        key?.let {
            CacheItemEvictedEvent(source = "CaffeineCache",
                                  key = it.toString(),
                                  value = value,
                                  cause = cause)
        }?.let { publisher.publishEvent(it) }// Publish a standard Spring event
    }
}

@SpringBootApplication(scanBasePackages = ["cfcodefans.study.spring_field.spring.examples.cache"])
@EnableAutoConfiguration(exclude = [//GsonAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
@EnableCaching
@Configuration
open class SpringCacheApp {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SpringCacheApp::class.java)
    }

    @Bean
    open fun cacheMgr(removalListener: CaffeineRemovalListenerAdapter): CacheManager = CaffeineCacheManager("file", "content")
        .apply {
            this.setCaffeine(Caffeine
                                 .newBuilder()
                                 .expireAfterWrite(5, TimeUnit.SECONDS)
                                 .removalListener(removalListener))
            log.info("initiate CacheManager...")
        }
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SpringCacheApp::class],
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                properties = ["spring.profiles.active="],
                useMainMethod = UseMainMethod.WHEN_AVAILABLE)
open class SpringCacheTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SpringCacheTests::class.java)

        fun CacheManager.info(): String = mapOf("caches" to cacheNames.map { cn ->
            getCache(cn)
        }.map { c ->
            mapOf("name" to c.name,
                  "clzz" to c.javaClass.name,
                  "native" to c.nativeCache.javaClass.name)
        }).let { Jsons.toString(it) }
    }

    @Autowired
    open lateinit var cacheMgr: CacheManager

    @Autowired
    open lateinit var fileCacheService: FileCacheService

    @Test
    open fun testCacheMgr() {
        assertNotNull(cacheMgr)
        assertNotNull(fileCacheService)

        log.info(cacheMgr.info())
    }

    @Test
    open fun testCacheService() {
        assertNotNull(fileCacheService)
        Path.of("./").toAbsolutePath().toString().let { log.info(it) }
        var filename: String = Path.of("./pom.xml").toAbsolutePath().toString()

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }

        log.info(cacheMgr.info())

        fileCacheService.evictContent(filename)
            .also { assertNotNull(it) }

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }
    }

    @Test
    open fun `test ttl of 2 sec`() {
        assertNotNull(fileCacheService)
        Path.of("./").toAbsolutePath().toString().let { log.info(it) }
        var filename: String = Path.of("./pom.xml").toAbsolutePath().toString()

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }

        MiscUtils.sleep(6000)
        log.info("waited 6 seconds")

        fileCacheService.getContent(filename)
            .also { assertTrue(it?.isNotEmpty() == true) }
            .let { log.info("loaded ${it?.size ?: 0} from $filename") }
    }
}