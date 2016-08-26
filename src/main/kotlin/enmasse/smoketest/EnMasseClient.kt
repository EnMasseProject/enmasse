package enmasse.smoketest

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.jms.*
import javax.naming.Context

/**
 * @author Ulf Lilleengen
 */
class EnMasseClient(val context: Context) {

    fun recvMessages(address: String, numMessages: Int, connectTimeout: Long = 5, timeUnit: TimeUnit = TimeUnit.MINUTES, connectListener: () -> Unit = {}): List<String> {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectWithTimeout(connectionFactory, connectTimeout, timeUnit)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val consumer = session.createConsumer(destination)


        val latch = CountDownLatch(numMessages)
        var received = java.util.ArrayList<String>()
        consumer.setMessageListener { message ->
            received.add(message.toString())
            latch.countDown()
        }
        Thread.sleep(1000) // TODO: Figure out why this is needed :(
        println("Receiver set up, allowing sender to start")
        connectListener.invoke()
        latch.await()

        connection.close()
        return received
    }

    fun sendMessages(address: String, messages: List<String>, connectTimeout: Long = 5, timeUnit: TimeUnit = TimeUnit.MINUTES): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectWithTimeout(connectionFactory, connectTimeout, timeUnit)
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val messageProducer = session.createProducer(destination)

        var messagesSent = 0
        for (msg in messages) {
            val message = session.createTextMessage(msg)
            message.jmsCorrelationID = "${++messagesSent}"
            println("Sending message with id ${messagesSent}")
            messageProducer.send(message, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE)
        }

        connection.close()
        return messagesSent
    }
}
