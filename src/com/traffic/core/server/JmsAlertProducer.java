package com.traffic.core.server;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class JmsAlertProducer {

    private static String url = ActiveMQConnectionFactory.DEFAULT_BROKER_URL;
    private static String subject = "TRAFFIC_ALERTS_QUEUE";

    public void sendSpeedingAlert(String vehicleId, double speed) {
        try {
            // 1. Connect to ActiveMQ
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // 2. Create Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(subject);
            MessageProducer producer = session.createProducer(destination);

            // 3. Create Text Message
            TextMessage message = session.createTextMessage("ALERT: Vehicle " + vehicleId + " is speeding at " + speed + " km/h!");

            // 4. Send
            producer.send(message);
            System.out.println(">>> [JMS] Sent Speeding Alert for " + vehicleId);

            connection.close();
        } catch (Exception e) {
            System.err.println("[JMS ERROR] Is ActiveMQ running? " + e.getMessage());
        }
    }
}