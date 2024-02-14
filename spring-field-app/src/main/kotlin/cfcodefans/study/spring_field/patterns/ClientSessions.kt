package cfcodefans.study.spring_field.patterns

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * refers to https://github.com/iluwatar/java-design-patterns/tree/master/client-session
 * Intent:
 *  1. Create stateless servers that removes the problem of clustering, as users can switch between servers seamlessly.
 *  2. Makes data more resilient in case of server fail-over.
 *  3. Works well with smaller data sizes
 */
object ClientSessions {
    private val log: Logger = LoggerFactory.getLogger(ClientSessions::class.java)

    data class Session(var id: String, var clientName: String)

    data class Request(val data: String, val session: Session)

    data class Server(val host: String, val port: Int) {
        fun getSession(name: String): Session = Session(id = UUID.randomUUID().toString(), clientName = name)
        fun process(req: Request) {
            log.info("Processing Request with client: ${req.session.clientName} data: ${req.data}")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val server: Server = Server("localhost", 8080)
        val session1: Session = server.getSession("session1")
        val session2: Session = server.getSession("session2")
        val req1: Request = Request("data1", session1)
        val req2: Request = Request("data2", session2)
        server.process(req1)
        server.process(req2)
    }
}