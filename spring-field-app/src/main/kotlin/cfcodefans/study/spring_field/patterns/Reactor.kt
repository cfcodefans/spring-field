package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.*
import java.util.concurrent.*


/**
 * Refers to https://github.com/iluwatar/java-design-patterns/blob/master/reactor/README.md
 * Intent:
 *  The reactor design pattern handles service requests that are delivered concurrently to an application
 *  by one or more clients. The application can register specific handlers for processing
 *  which are called by reactor on specific events. Dispatching of event handlers is performed by an
 *  initiation dispatcher, which manages the registered event handlers. Demultiplexing of service requests
 *  is performed by a synchronous event demultiplexer.
 */
object Reactor {
    private val log: Logger = LoggerFactory.getLogger(Reactor::class.java)

    fun interface IChannelHandler {
        fun handleChannelRead(channel: AbstractNioChannel,
                              readObj: Any,
                              key: SelectionKey)
    }

    interface IDispatcher {
        fun onChannelReadEvent(channel: AbstractNioChannel,
                               readObj: Any,
                               key: SelectionKey)

        @Throws(InterruptedException::class)
        fun stop()
    }

    data class ChangeKeyOpsCmd(private val key: SelectionKey,
                               private val interestedOps: Int) : Runnable {
        override fun run() {
            key.interestOps(interestedOps)
        }

        override fun toString(): String = "Change of ops to: $interestedOps"
    }

    open class NioReactor(private val dispatcher: IDispatcher) {
        private val selector: Selector = Selector.open()

        private val pendingCmds: Queue<Runnable> = ConcurrentLinkedQueue()
        val reactorMain: ExecutorService = Executors.newSingleThreadExecutor()

        open fun start(): Unit {
            reactorMain.execute {
                try {
                    log.info("Reactor started, waiting for events....")
                    eventLoop()
                } catch (e: IOException) {
                    log.error("Exception in event loop", e)
                }
            }
        }

        @Throws(InterruptedException::class, IOException::class)
        fun stop(): Unit {
            reactorMain.shutdown()
            selector.wakeup()
            if (reactorMain.awaitTermination(4, TimeUnit.SECONDS).not()) {
                reactorMain.shutdownNow()
            }
            selector.close()
            log.info("Reactor stopped")
        }

        /**
         * Register a new channel (handle) with this reactor. Reactor will start waiting for events on
         * this channel and notify of any events. While registering the channel the reactor uses {@link
         * AbstractNioChannel#getInterestedOps()} to know about the interested operation of this channel.
         *
         * @param channel a new channel on which reactor will wait for events. The channel must be bound prior to being registered.
         * @return this
         */
        @Throws(IOException::class)
        fun registerChannel(channel: AbstractNioChannel): NioReactor = apply {
            val key: SelectionKey = channel.getJavaChannel().register(selector, channel.getInterestedOps())
            key.attach(channel)
            channel.reactor = this
        }

        fun changeOps(key: SelectionKey, interestedOps: Int) {
            pendingCmds.add(ChangeKeyOpsCmd(key, interestedOps))
            selector.wakeup()
        }

        @Throws(IOException::class)
        private fun eventLoop(): Unit {
            while (Thread.interrupted().not()) { //honor interrupt request
                processPendingCmds() // Honor any pending commands first
                /**
                 * Synchronous event de-multiplexing happens here, this is blocking call which returns
                 * when it is possible to initiate non-blocking operation on any of the registered channels.
                 */
                selector.select()
                // Represents the events that have occurred on registered handles.
                val keys: MutableSet<SelectionKey> = selector.selectedKeys()
                keys.filter { k -> k.isValid }
                    .forEach { k ->
                        if (k.isAcceptable) {
                            (k.channel() as ServerSocketChannel)
                                .accept()
                                .configureBlocking(false)
                                .register(selector, SelectionKey.OP_READ)
                                .attach(k.attachment())
                        } else if (k.isReadable) {
                            try {
                                val channel: AbstractNioChannel = k.attachment() as AbstractNioChannel
                                dispatcher.onChannelReadEvent(channel, channel.read(k), k)
                            } catch (e: IOException) {
                                try {
                                    k.channel().close()
                                } catch (e1: IOException) {
                                    log.error("error closing channel", e)
                                }
                            }
                        } else if (k.isWritable) {
                            (k.attachment() as AbstractNioChannel).flush(k)
                        }
                    }
                keys.clear()
            }
        }


        private fun processPendingCmds() {
            val itr: MutableIterator<Runnable> = pendingCmds.iterator()
            while (itr.hasNext()) {
                val cmd: Runnable = itr.next()
                cmd.run()
                itr.remove()
            }
        }
    }

    abstract class AbstractNioChannel(private val channel: SelectableChannel,
                                      val handler: IChannelHandler) {

        private val channelToPendingWrites: MutableMap<SelectableChannel, Queue<Any>> = ConcurrentHashMap()
        var reactor: NioReactor? = null

        open fun getJavaChannel(): SelectableChannel = channel

        abstract fun getInterestedOps(): Int

        @Throws(IOException::class)
        abstract fun bind()

        /**
         * Reads the data using the key and returns the read data.
         * The underlying channel should be fetched using SelectionKey#channel()
         *
         * @param key the key on which read event occurred.
         * @return data read.
         * @throws IOException if any I/O error occurs.
         */
        @Throws(IOException::class)
        abstract fun read(key: SelectionKey): Any

        @Throws(IOException::class)
        abstract fun doWrite(pendingWrite: Any, key: SelectionKey): Unit

        @Throws(IOException::class)
        fun flush(key: SelectionKey): Unit {
            val pendingWrites: Queue<Any> = channelToPendingWrites[key.channel()] ?: return
            var pendingWrite: Any? = pendingWrites.peek()
            while (pendingWrite != null) {
                doWrite(pendingWrite, key)
                pendingWrite = pendingWrites.peek()
            }
//There is nothing else to write, so channel is interested in reading more data.
            reactor!!.changeOps(key, SelectionKey.OP_READ)
        }

        fun write(data: Any, key: SelectionKey): Unit {
            channelToPendingWrites[key.channel()]
                ?: run {
                    synchronized(channelToPendingWrites) {
                        channelToPendingWrites.computeIfAbsent(key.channel()) {
                            ConcurrentLinkedQueue<Any>()
                        }
                    }
                }.add(data)
            reactor!!.changeOps(key, SelectionKey.OP_WRITE)
        }
    }

    open class NioServerSocketChannel(private val port: Int,
                                      handler: IChannelHandler) : AbstractNioChannel(handler = handler, channel = ServerSocketChannel.open()) {

        override fun getInterestedOps(): Int = SelectionKey.OP_ACCEPT
        override fun getJavaChannel(): ServerSocketChannel = super.getJavaChannel() as ServerSocketChannel

        @Throws(IOException::class)
        override fun read(key: SelectionKey): ByteBuffer {
            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            val read: Int = (key.channel() as SocketChannel).read(buffer)
            buffer.flip()
            if (read == -1) {
                throw IOException("Socket closed")
            }
            return buffer
        }

        @Throws(IOException::class)
        override fun bind() {
            getJavaChannel()
                .apply { socket().bind(InetSocketAddress(InetAddress.getLocalHost(), port)) }
                .configureBlocking(false)
            log.info("Bound TCP socket at port: $port")
        }

        @Throws(IOException::class)
        override fun doWrite(pendingWrite: Any, key: SelectionKey) {
            (key.channel() as SocketChannel).write(pendingWrite as ByteBuffer)
        }
    }

    class DatagramPacket(val data: ByteBuffer,
                         var sender: SocketAddress? = null,
                         var reciever: SocketAddress? = null)

    open class NioDatagramChannel(private val port: Int,
                                  handler: IChannelHandler) : AbstractNioChannel(handler = handler, channel = DatagramChannel.open()) {

        override fun getInterestedOps(): Int = SelectionKey.OP_READ
        override fun getJavaChannel(): DatagramChannel = super.getJavaChannel() as DatagramChannel

        @Throws(IOException::class)
        override fun read(key: SelectionKey): DatagramPacket {
            val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            val sender: SocketAddress = (key.channel() as DatagramChannel).receive(buffer)
            buffer.flip()
            return DatagramPacket(data = buffer, sender = sender)
        }

        @Throws(IOException::class)
        override fun bind() {
            getJavaChannel()
                .apply { socket().bind(InetSocketAddress(InetAddress.getLocalHost(), port)) }
                .configureBlocking(false)
            log.info("Bound UDP socket at port: $port")
        }

        @Throws(IOException::class)
        override fun doWrite(pendingWrite: Any, key: SelectionKey) {
            val pendingPacket: DatagramPacket = (pendingWrite as DatagramPacket)
            getJavaChannel().send(pendingPacket.data, pendingPacket.reciever)
        }
    }

    open class ThreadPoolDispatcher(poolSize: Int) : IDispatcher {
        private val executorService: ExecutorService = Executors.newFixedThreadPool(poolSize)

        override fun onChannelReadEvent(channel: AbstractNioChannel,
                                        readObj: Any,
                                        key: SelectionKey) {
            executorService.execute { channel.handler.handleChannelRead(channel, readObj, key) }
        }

        @Throws(InterruptedException::class)
        override fun stop() {
            executorService.shutdown()
            if (executorService.awaitTermination(4, TimeUnit.SECONDS).not()) {
                executorService.shutdownNow()
            }
        }
    }

    open class SameThreadDispatcher : IDispatcher {
        override fun onChannelReadEvent(channel: AbstractNioChannel,
                                        readObj: Any,
                                        key: SelectionKey) {
            channel.handler.handleChannelRead(channel, readObj, key)
        }

        override fun stop() {}
    }

    object LoggingHandler : IChannelHandler {
        private val log: Logger = LoggerFactory.getLogger(LoggingHandler::class.java)

        val ACK: ByteArray = "Data logged successfully".toByteArray()

        override fun handleChannelRead(channel: AbstractNioChannel, readObj: Any, key: SelectionKey) {
            when (readObj) {
                is ByteBuffer -> {
                    doLogging(readObj)
                    sendReply(channel, key)
                }
                is DatagramPacket -> {
                    doLogging(readObj.data)
                    sendReply(channel = channel, incomingPacket = readObj, key = key)
                }
                else -> throw IllegalStateException("Unknown data: $readObj received")
            }
        }

        private fun sendReply(channel: AbstractNioChannel, key: SelectionKey) {
            channel.write(ByteBuffer.wrap(ACK), key)
        }

        private fun sendReply(channel: AbstractNioChannel,
                              incomingPacket: DatagramPacket,
                              key: SelectionKey) {
            // create a reply acknowledgement datagram packet setting the receiver to the message
            channel.write(DatagramPacket(data = ByteBuffer.wrap(ACK),
                    reciever = incomingPacket.sender),
                    key)
        }

        private fun doLogging(data: ByteBuffer): Unit {
            log.info(String(data.array(), 0, data.limit()))
        }
    }


}