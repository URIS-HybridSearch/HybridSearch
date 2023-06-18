package com.urisproject.facesearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class WebSocketConfig {

    // Enable WebSocket endpoints using the default Spring WebSocket configuration
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    // Configure the WebSocket container with a maximum buffer size and session idle timeout
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(512000); // Set the maximum buffer size for text messages
        container.setMaxBinaryMessageBufferSize(512000); // Set the maximum buffer size for binary messages
        container.setMaxSessionIdleTimeout(15 * 60000L); // Set the maximum session idle timeout to 15 minutes
        return container;
    }

}