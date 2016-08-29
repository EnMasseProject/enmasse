package enmasse.smoketest

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import javax.jms.*
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
class EnMasseClient(val context: Context, numThreads: Int, val synchronizeClients: Boolean) {
    val barrier = CyclicBarrier(numThreads)

    fun recvMessages(address: String, numMessages: Int, connectTimeout: Long = 1, timeUnit: TimeUnit = TimeUnit.MINUTES): List<String> {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectWithTimeout(connectionFactory, connectTimeout, timeUnit)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val consumer = session.createConsumer(destination)

        val latch = CountDownLatch(numMessages)

        val received = ArrayList<String>()
        consumer.setMessageListener { message ->
            received.add(message.toString())
            latch.countDown()
        }

        println("Receiver set up, allowing sender to start")
        if (synchronizeClients) {
            barrier.await(30, TimeUnit.SECONDS)
        }

        println("Waiting for messages")
        latch.await()

        println("Received ${received.size} messages")
        connection.close()
        return received
    }

    fun sendMessages(address: String, messages: List<String>, connectTimeout: Long = 1, timeUnit: TimeUnit = TimeUnit.MINUTES): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        if (synchronizeClients) {
            barrier.await(30, TimeUnit.SECONDS)
        }

        val connection = connectWithTimeout(connectionFactory, connectTimeout, timeUnit)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val messageProducer = session.createProducer(destination)

        println("Starting sender")

        var messagesSent = 0
        for (msg in messages) {
            val message = session.createTextMessage(msg)
            messageProducer.send(message, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE)
            messagesSent++
        }

        println("Sent ${messagesSent} messages")

        connection.close()
        return messagesSent
    }


}
