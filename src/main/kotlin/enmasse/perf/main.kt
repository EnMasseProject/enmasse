package enmasse.perf

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.IClient
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IService
import io.vertx.core.Vertx
import io.vertx.proton.ProtonClient
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message
import java.util.concurrent.atomic.AtomicLong

public fun main(args: Array<String>) {
    val user = System.getenv("OPENSHIFT_USER")
    val token = System.getenv("OPENSHIFT_TOKEN")
    val url = System.getenv("OPENSHIFT_URL")
    val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();

    val tester = Tester(client)
    tester.runTest("anycast")
    tester.runTest("myqueue")

}

class Tester(val client: IClient) {
    val namespace = "enmasse-ci"
    val numReceived = AtomicLong()

    fun getMessagingService(): IService {
        return client.get(ResourceKind.SERVICE, "messaging", namespace)
    }

    fun runTest(address: String) {
        val vertx = Vertx.vertx()
        val client = ProtonClient.create(vertx)
        val service = getMessagingService()

        println("Running test against ${service.portalIP}:${service.port}/${address}")
        numReceived.set(0)
        connectAndRunTest(address, client, service, vertx)
        while (numReceived.get() < 10) {
            Thread.sleep(1000);
        }
        vertx.close()
    }

    fun connectAndRunTest(address: String, client: ProtonClient, service: IService, vertx: Vertx) {
        val msgnum = AtomicLong(0)
        client.connect(service.portalIP, service.port, { handle ->
            if (handle.succeeded()) {
                println("${System.currentTimeMillis()}: successfully connected")
                val connection = handle.result().open()
                val receiver = connection.createReceiver(address).handler { protonDelivery, message ->
                    println("Message received: ${message.body.toString()}")
                    numReceived.incrementAndGet()
                }.open()
                val sender = connection.createSender(address).open()
                vertx.setPeriodic(1000, { timerId ->
                    val message = Message.Factory.create()
                    message.setBody(AmqpValue("Hello, receiver, msg ${msgnum.incrementAndGet()}!"))
                    sender.send(message)
                })
            } else {
                println("${System.currentTimeMillis()}: error connecting, retrying in 10 seconds")
                vertx.setTimer(10000, { timerId ->
                    connectAndRunTest(address, client, service, vertx)
                })
            }
        })
    }
}
