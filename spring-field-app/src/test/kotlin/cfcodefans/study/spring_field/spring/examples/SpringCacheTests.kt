package cfcodefans.study.spring_field.spring.examples.cache

import cfcodefans.study.spring_field.commons.Jsons
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
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Path
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

}

@SpringBootApplication(scanBasePackages = ["cfcodefans.study.spring_field.spring.examples.cache"])
@EnableAutoConfiguration(exclude = [GsonAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class
])
@EnableCaching
@Configuration
open class SpringCacheApp {
//    @Bean
//    open fun cacheMgr(): CacheManager = CaffeineCacheManager()
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
}