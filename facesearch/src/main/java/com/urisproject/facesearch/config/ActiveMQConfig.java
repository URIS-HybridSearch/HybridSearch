package com.urisproject.facesearch.config;

import jakarta.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.SingleConnectionFactory;

@Configuration
public class ActiveMQConfig {

    // Define a bean for the message queue
    @Bean
    public Queue queue() {
        return new ActiveMQQueue("ActiveMQQueue"); // Set the name of the queue
    }



}