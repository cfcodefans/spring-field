package cfcodefans.study.spring_field.middlewares

import com.thenetcircle.commons.Jsons
import org.apache.zookeeper.AddWatchMode
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.Stat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ZookeeperTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(ZookeeperTests::class.java)
        val HOST_NAME: String = "172.19.133.151"
        val TIMEOUT: Int = 1000

        fun interface IWatcher : Watcher, Function1<WatchedEvent?, Unit> {
            override fun process(ev: WatchedEvent?) = invoke(ev)
        }

        val LOG_WATCHER: (WatchedEvent?) -> Unit = { event: WatchedEvent? ->
            log.info("zookeeper event: $event")
        }

        val zookeeperUrl: String = "$HOST_NAME:12181,$HOST_NAME:22181,$HOST_NAME:32181"
        var zk: ZooKeeper = ZooKeeper(zookeeperUrl, TIMEOUT, LOG_WATCHER)
        val TEST_PATH_ROOT: String = "/spring-field"

        @AfterAll
        @JvmStatic
        fun cleanUp() = zk.close()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            if (zk.exists(TEST_PATH_ROOT, false) != null) return
            log.info("creating the default path: $TEST_PATH_ROOT")
            val pathStr: String = zk.create(TEST_PATH_ROOT, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
            log.info("created $pathStr")
        }
    }

    @Test
    open fun testConnectivity() {
        log.info("state: ${zk.state}")
    }

    @Test
    fun testInfo() {
        log.info("Stat: ${Jsons.toString(zk.exists(TEST_PATH_ROOT, null))}")
        log.info("root: ${zk.getChildren(TEST_PATH_ROOT, LOG_WATCHER).joinToString(separator = "\n", prefix = "\n")}")
    }

    @Test
    fun testWatcher() {
        val records: MutableList<String> = arrayListOf()
        zk.addWatch(TEST_PATH_ROOT,
                { ev: WatchedEvent? ->
                    LOG_WATCHER(ev)
                    if (ev?.path?.startsWith(TEST_PATH_ROOT) == false) return@addWatch
                    records.add(ev!!.type.toString())
                },
                AddWatchMode.PERSISTENT_RECURSIVE)

        val fooPath: String = zk.create(TEST_PATH_ROOT + "/foo", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
            .also { log.info("created $it") }

        Thread.sleep(100)
        log.info("root: ${zk.getChildren(TEST_PATH_ROOT, LOG_WATCHER).joinToString(separator = "\n", prefix = "\n")}")
        log.info("records, ${records.joinToString(prefix = "\n", separator = "\n")}")
        Assertions.assertTrue(records.contains(EventType.NodeCreated.toString()))

        val barPath: String = zk.create(fooPath + "/bar", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
            .also { log.info("created $it") }
        Thread.sleep(100)
        log.info("root: ${zk.getChildren(TEST_PATH_ROOT, LOG_WATCHER).joinToString(separator = "\n", prefix = "\n")}")
        log.info("records, ${records.joinToString(prefix = "\n", separator = "\n")}")
        Assertions.assertTrue(records.count { it == EventType.NodeCreated.toString() } == 2)

        zk.delete(barPath, -1)
        Thread.sleep(100)
        log.info("root: ${zk.getChildren(TEST_PATH_ROOT, LOG_WATCHER).joinToString(separator = "\n", prefix = "\n")}")
        log.info("records, ${records.joinToString(prefix = "\n", separator = "\n")}")
        Assertions.assertTrue(records.count { it == EventType.NodeDeleted.toString() } == 1)

        zk.delete(TEST_PATH_ROOT + "/foo", -1)
    }
}