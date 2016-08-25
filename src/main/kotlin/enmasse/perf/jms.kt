package enmasse.perf

import java.util.*
import java.util.concurrent.TimeUnit
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.JMSException
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
fun createQueueContext(endpoint: Endpoint, address: String): Context {
    val env = Hashtable<Any, Any>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory")
    env.put("connectionfactory.enmasse", "amqp://${endpoint.host}:${endpoint.port}")
    env.put("queue.${address}", address)
    env.put("jms.connectTimeout", 60)
    return javax.naming.InitialContext(env);
}

fun connectWithTimeout(connectionFactory: ConnectionFactory, timeout: Long, unit: TimeUnit): Connection {
    val endTime = System.currentTimeMillis() + unit.toMillis(timeout)
    while (System.currentTimeMillis() < endTime) {
        try {
            return connectionFactory.createConnection()
        } catch (e: JMSException) {
            println("Error connecting, retrying in 2 seconds")
            Thread.sleep(2000)
        }
    }
    throw RuntimeException("Timed out when connecting to server")
}
