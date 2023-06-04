package com.urisproject.facesearch.web;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a message sent over a WebSocket connection.
 */
@Data
@AllArgsConstructor
public class SocketMessage {
    private String messageCode; // 100 represents data statistics, 200 represents log records, 300 represents data returned
    private String messageInfo; // Message information
    private String oldPhoto; // Base64-encoded image data for the user's old photo
    private String newPhoto; // Base64-encoded image data for the user's new photo
    private String allNum; // Total number of users in the system
}
