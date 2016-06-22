package enmasse.perf

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IService
import io.vertx.core.Vertx
import io.vertx.proton.ProtonClient
import io.vertx.proton.ProtonConnection
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message
import java.util.concurrent.atomic.AtomicLong

public fun main(args: Array<String>) {
    val user = System.getenv("OPENSHIFT_USER")
    val token = System.getenv("OPENSHIFT_TOKEN")
    val url = System.getenv("OPENSHIFT_URL")
    val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();
    val namespace = "enmasse-ci"
    val service: IService = client.get(ResourceKind.SERVICE, "messaging", namespace)
    val vertx = Vertx.vertx()

    val tester = Tester(vertx, service)
    try {
        tester.runTest("anycast", 10, 300L)
        tester.runTest("myqueue", 10, 300L)
    } finally {
        vertx.close()
    }

}

class Tester(val vertx: Vertx, val service: IService) {
    val client = ProtonClient.create(vertx)

    fun runTest(address: String, numMsg: Int, timeout: Long) {
        println("Running test against ${service.portalIP}:${service.port}/${address}")
        val numReceived = AtomicLong()
        val endTime = System.currentTimeMillis() + (timeout * 1000)
        connectAndRunTest(address, { connection: ProtonConnection ->
            println("${System.currentTimeMillis()}: successfully connected")
            val receiver = connection.createReceiver(address)
            receiver.handler { protonDelivery, message ->
                println("Message received: ${message.body.toString()}")
                numReceived.incrementAndGet()
                if (numReceived.get() >= numMsg) {
                    receiver.close()
                    connection.close()
                }
            }.open()

            val sender = connection.createSender(address).open()
            var sendNum = 0
            vertx.setPeriodic(1000, { timerId ->
                val message = Message.Factory.create()
                message.setBody(AmqpValue("Hello, receiver, msg ${sendNum++}"))
                sender.send(message)
                if (sendNum >= numMsg) {
                    vertx.cancelTimer(timerId)
                    sender.close()
                }
            })
        })

        while (numReceived.get() < numMsg && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
        }

        if (numReceived.get() < numMsg) {
            throw RuntimeException("Number of messages received (${numReceived.get()}) does not meet expected (${numMsg})")
        }
    }

    fun connectAndRunTest(address: String, handler: (ProtonConnection) -> Unit) {
        client.connect(service.portalIP, service.port, { handle ->
            if (handle.succeeded()) {
                handler.invoke(handle.result().open())
            } else {
                println("${System.currentTimeMillis()}: error connecting, retrying in 10 seconds")
                vertx.setTimer(10000, { timerId ->
                    connectAndRunTest(address, handler)
                })
            }
        })
    }
}
