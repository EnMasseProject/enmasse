package enmasse.perf

import com.openshift.internal.restclient.ResourceFactory
import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.IClient
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IResource
import com.openshift.restclient.model.IService
import io.vertx.core.Vertx
import io.vertx.proton.ProtonClient
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message
import java.util.*

public fun main(args: Array<String>) {
    val user = System.getenv("OPENSHIFT_USER")
    val token = System.getenv("OPENSHIFT_TOKEN")
    val url = System.getenv("OPENSHIFT_URL")
    val client = ClientBuilder(url).authorizationStrategy(TokenAuthorizationStrategy(token, user)).build();
    val rf = ResourceFactory(client)

    Tester(client, rf).runTest()

}

class Tester(val client: IClient, val rf: ResourceFactory) {
    val resourceNames = listOf("configmap-bridge-rc.json", "configuration-service.json", "messaging-service.json", "qdrouterd-rc.json", "ragent-rc.json", "ragent-service.json", "rc-generator-rc.json")
    val namespace = "enmasse-ci"

    fun createResources(): List<IResource> {
        return resourceNames.map { r ->
            val resource = createResource(r)
            val handle = client.create(resource, namespace)
            System.out.println("Created resource ${resource.name}")
            handle
        }.toList()
    }

    fun tryDelete(kind: String) {
        try {
            client.list<IResource>(kind, namespace).forEach { r -> client.delete(r) }
        } catch (e: Exception) {
            println("Error deleting ${kind}. Ignoring")
        }
    }

    fun deleteAllResources() {
        tryDelete(ResourceKind.REPLICATION_CONTROLLER);
        tryDelete(ResourceKind.POD);
        tryDelete(ResourceKind.CONFIG_MAP);
        tryDelete(ResourceKind.SERVICE);
    }

    fun createResource(uri: String): IResource {
        val rstream = ClassLoader.getSystemResourceAsStream(uri)
        return rf.create<IResource>(rstream)
    }

    fun runTest() {
        deleteAllResources()

        Thread.sleep(10000)

        val resources = createResources()

        println("Created resources, waiting for system to come up")
        Thread.sleep(60000)

        val initialMap = createResource("addresses_noqueue.json")
        client.create(initialMap, namespace)

        println("Running send test")
        runSendTest()

        val empty = createResource("addresses_empty.json")
        client.update(empty)

        println("Deleted address definition, waiting")
        Thread.sleep(10000)

        deleteAllResources()
    }

    fun getMessagingService(): IService {
        return client.get(ResourceKind.SERVICE, "messaging", namespace)
    }

    fun runSendTest() {
        val vertx = Vertx.vertx()
        val client = ProtonClient.create(vertx)
        val service = getMessagingService()

        println("Attempting to connect on ${service.portalIP}:${service.port}")
        connectAndRunTest(client, service, vertx)
        Thread.sleep(60000);
        vertx.close()
    }

    fun connectAndRunTest(client: ProtonClient, service: IService, vertx: Vertx) {
         client.connect(service.portalIP, service.port, { handle ->
            if (handle.succeeded()) {
                val connection = handle.result().open()
                val receiver = connection.createReceiver("anycast").handler { protonDelivery, message ->
                    println("Message received: ${message.body.toString()}")
                }.open()
                val sender = connection.createSender("anycast").open()
                val message = Message.Factory.create()
                message.setBody(AmqpValue("Hello, receiver!"))
                vertx.setPeriodic(1000, { timerId ->
                    sender.send(message)
                })
            } else {
                println("Error connecting, retrying in 1 second")
                vertx.setTimer(1000, { timerId ->
                    connectAndRunTest(client, service, vertx)
                })
            }
        })
    }
}
