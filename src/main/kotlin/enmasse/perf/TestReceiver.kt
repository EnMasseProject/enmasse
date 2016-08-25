package enmasse.perf

import javax.jms.ConnectionFactory
import javax.jms.Destination
import javax.jms.JMSException
import javax.jms.Session
import javax.naming.Context

class TestReceiver(val context: Context) {

    fun recvMessages(numMessages: Int, address: String): Int {
        val connectionFactory = context.lookup("enmasse") as ConnectionFactory
        val destination = context.lookup(address) as Destination

        val connection = connectionFactory.createConnection()
        connection.start();

        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val consumer = session.createConsumer(destination)

        var numReceived = 0
        for (i in 1.rangeTo(numMessages)) {
            val message = consumer.receive()
            val cid = "${i}"
            if (!cid.equals(message.jmsCorrelationID)) {
                throw IllegalStateException("Got messages in wrong order")
            }
            numReceived++
        }


        consumer.close()
        session.close()
        connection.close()
        return numReceived
    }
}