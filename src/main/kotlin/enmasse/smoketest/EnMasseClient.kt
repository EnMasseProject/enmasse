package enmasse.smoketest

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

        connectListener.invoke()
        var numReceived = 0
        val receivedMessages = 1.rangeTo(numMessages).map { i ->
            val message = consumer.receive()
            numReceived++
            message.toString()
        }

        consumer.close()
        session.close()
        connection.close()
        return receivedMessages
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
            messageProducer.send(message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE)
        }

        messageProducer.close()
        session.close()
        connection.close()
        return messagesSent
    }
}
