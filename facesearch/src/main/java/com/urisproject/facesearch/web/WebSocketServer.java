package com.urisproject.facesearch.web;

import com.alibaba.fastjson.JSON;
import com.urisproject.facesearch.common.util.SpringContext;
import com.urisproject.facesearch.model.User;
import com.urisproject.facesearch.model.face.FaceSearchHandler;
import com.urisproject.facesearch.pojo.form.FaceSearchForm;
import io.micrometer.common.util.StringUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description: web socket endpoint for handling real-time message.
 */
@Component
@Log4j2
@ServerEndpoint("/api/pushMessage/{userId}")
public class WebSocketServer {
    // keep track of the number of online connections.
    // and a map
    private static int onlineCount;
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    // Instant variables for storing current session and user ID.
    private Session session;
    private String userId = "";

    /**
     * Method called when a new WebSocket connection is established.
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
        }
        webSocketMap.put(userId, this);
        addOnlineCount();
        log.info("User connected: " + userId + ", current online count: " + getOnlineCount());
        sendMessage("Connection established.");
    }

    /**
     * Method called when a WebSocket connection is closed.
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            subOnlineCount();
        }
        log.info("User disconnected: " + userId + ", current online count: " + getOnlineCount());
    }

    /**
     * Method called when a message is received from a WebSocket client.
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        FaceSearchHandler faceSearchHandler = SpringContext.getBean(FaceSearchHandler.class);
        FaceSearchForm faceSearchForm = JSON.parseObject(message, FaceSearchForm.class);
        if (StringUtils.isBlank(faceSearchForm.getImageBase64())) {
            log.info("Received blank message.");
        } else {
            log.info("Processing face search message.");
            try {
                List<User> users = faceSearchHandler.faceHandler(faceSearchForm.getImageBase64());
                if (!users.isEmpty()) {
                    WebSocketServer.sendInfo(JSON.toJSONString(users), faceSearchForm.getSessionId());
                }
            } catch (IOException e) {
                log.error("Error handling face search message: " + e.getMessage());
            }
        }
    }

    /**
     * Method called when an error occurs in the WebSocket connection.
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error in WebSocket connection for user " + userId + ": " + error.getMessage());
    }

    /**
     * Method for sending a message to the current WebSocket client.
     */
    public void sendMessage(String message) {
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            log.error("Error sending message to user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Method for sending a message to a specific WebSocket client.
     */
    public static void sendInfo(String message, String userId) {
        if (StringUtils.isNotBlank(userId) && webSocketMap.containsKey(userId)) {
            WebSocketServer webSocketServer = webSocketMap.get(userId);
            webSocketServer.sendMessage(message);
        } else {
            log.error("User " + userId + " is not online.");
        }
    }

    /**
     * Method for getting the current number of online connections.
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * Method for adding one to the current number of online connections.
     */
    public static synchronized void addOnlineCount() {
        onlineCount++;
    }

    /**
     * Method for subtracting one from the current number of online connections.
     */
    public static synchronized void subOnlineCount() {
        onlineCount--;
    }

}