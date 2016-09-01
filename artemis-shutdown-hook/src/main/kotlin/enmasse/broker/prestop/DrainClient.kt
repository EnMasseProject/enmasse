package enmasse.broker.prestop

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.proton.ProtonClient
import io.vertx.proton.ProtonQoS
import org.apache.activemq.artemis.api.core.client.*
import org.apache.activemq.artemis.api.core.management.ManagementHelper
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Ulf Lilleengen
 */
class DrainClient(val mgmtEndpoint: Endpoint, val from: Endpoint, val address: String, val debugFn: Optional<() -> Unit>) {

    val vertx = Vertx.vertx()
    val client = ProtonClient.create(vertx)
    val locator:ServerLocator
    val sessionFactory:ClientSessionFactory
    val session:ClientSession

    init {
        locator = ActiveMQClient.createServerLocator("tcp://${mgmtEndpoint.hostName}:${mgmtEndpoint.port}")
        sessionFactory = locator.createSessionFactory()
        session = sessionFactory.createSession()
    }

    fun drainMessages(to: Endpoint) {

        vertx.setPeriodic(1000, { timerId ->
            vertx.executeBlocking<Int>({ future ->
                try {
                    val count = checkQueue()
                    println("Queue had ${count} messages")
                    if (count == 0) {
                        shutdownBroker()
                    }
                    future.complete(count)
                } catch (e: Exception) {
                    future.fail(e)
                }
            }, { res->
                System.exit(0)
            })
        })

        val first = AtomicBoolean(false)
        client.connect(to.hostName, to.port, { handle ->
            if (handle.succeeded()) {
                val sender = handle.result().open().createSender(address)
                        .setQoS(ProtonQoS.AT_LEAST_ONCE)
                        .open()
                client.connect(from.hostName, from.port, { handle ->
                    if (handle.succeeded()) {
                        val receiver = handle.result().open().createReceiver(address)
                                .setAutoAccept(false)
                                .open()
                        receiver.handler { protonDelivery, message ->

                            sender.send(message, { targetDelivery ->
                                protonDelivery.disposition(targetDelivery.remoteState, targetDelivery.remotelySettled())
                            })

                            // This is for debugging only
                            if (!first.getAndSet(true) && debugFn.isPresent) {
                                vertx.executeBlocking<Int>({ future -> debugFn.get().invoke(); future.complete(0) }, { handler: AsyncResult<Int> -> 0 })
                            }
                        }
                    }
                })
            }
        })
    }

    fun checkQueue(): Int {
        val requestor = ClientRequestor(session, "jms.queue.activemq.management")
        val message = session.createMessage(false)
        ManagementHelper.putAttribute(message, "core.queue.${address}", "messageCount")
        session.start()
        val reply = requestor.request(message)
        val count = ManagementHelper.getResult(reply)
        session.stop()
        return count as Int
    }

    fun listQueues() {
        val requestor = ClientRequestor(session, "jms.queue.activemq.management")
        val message = session.createMessage(false)
        ManagementHelper.putOperationInvocation(message, "core.server", "getQueueNames")
        session.start()
        val reply = requestor.request(message)
        val count = ManagementHelper.getResult(reply) as Array<Object>
        session.stop()
        count.forEach { o -> println(o) }
    }

    fun shutdownBroker() {
        println("Shutting down")
        val requestor = ClientRequestor(session, "jms.queue.activemq.management")
        val message = session.createMessage(false)
        ManagementHelper.putOperationInvocation(message, "core.server", "forceFailover")
        session.start()
        val reply = requestor.request(message)
        session.stop()
    }

}
