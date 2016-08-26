package enmasse.smoketest

import java.util.*
import java.util.concurrent.TimeUnit
import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */

private fun createCommonEnv(): Hashtable<Any, Any> {
    val endpoint = Environment.endpoint
    val env = Hashtable<Any, Any>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory")
    env.put("connectionfactory.enmasse", "amqp://${endpoint.host}:${endpoint.port}")
    env.put("jms.connectTimeout", 300)
    return env
}

fun createQueueContext(address: String): Context {
    val env = createCommonEnv()
    env.put("queue.${address}", address)
    return javax.naming.InitialContext(env);
}

fun createTopicContext(address: String): Context {
    val env = createCommonEnv()
    env.put("topic.${address}", address)
    return javax.naming.InitialContext(env);
}

fun connectWithTimeout(connectionFactory: ConnectionFactory, timeout: Long, unit: TimeUnit): Connection {
    val endTime = System.currentTimeMillis() + unit.toMillis(timeout)
    while (System.currentTimeMillis() < endTime) {
        try {
            return connectionFactory.createConnection()
        } catch (e: Exception) {
            println("Error connecting: ${e.message}, retrying in 2 seconds")
            Thread.sleep(2000)
        }
    }
    throw RuntimeException("Timed out when connecting to server")
}
