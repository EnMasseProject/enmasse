package enmasse.perf

import java.util.*
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
