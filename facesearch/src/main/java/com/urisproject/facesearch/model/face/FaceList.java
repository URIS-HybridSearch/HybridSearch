package com.urisproject.facesearch.model.face;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description:
 */


import com.alibaba.fastjson.JSON;
import com.arcsoft.face.FaceInfo;
import com.urisproject.facesearch.common.redis.RedisService;
import com.urisproject.facesearch.common.util.Base64Utils;
import com.urisproject.facesearch.common.util.SnowflakeIdWorker;
import com.urisproject.facesearch.pojo.form.MessageImageForm;
import com.urisproject.facesearch.pojo.form.UploadResultForm;
import com.urisproject.facesearch.web.SocketMessage;
import com.urisproject.facesearch.web.WebSocketServer;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
@Log4j2
@Component
public class FaceList {

    @Autowired
    private FaceUtils faceUtils;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Autowired
    private Queue queue;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Checks a list of image files for faces and sends notifications to the front-end.
     * @param listFiles The list of files to check.
     * @param sessionId The session ID of the WebSocket connection.
     * @return A list of results for each file checked.
     */
    @SneakyThrows
    public List<UploadResultForm> faceListCheck(File[] listFiles, String sessionId) {
        int isJpeg = 0;
        int noJpeg = 0;
        for (File f : listFiles) {
            // Check if the file is a regular file
            if (f.isFile()) {
                // Check if the file is a JPEG image
                if (f.getName().endsWith(".jpg")) {
                    isJpeg++;
                }
            } else if (f.isDirectory()) {
                noJpeg++;
            }
        }
        int all = isJpeg + noJpeg;
        log.info("Total number of files: " + all);

        WebSocketServer.sendInfo(JSON.toJSONString(
                        new SocketMessage("100",
                                "Files that match the format: " + isJpeg + "; Files that don't match: " + noJpeg,
                                null, null, String.valueOf(all))),
                sessionId);

        for (File f : listFiles) {
            // Check if the file is a regular file
            if (f.isFile()) {
                // Check if the file is a JPEG image
                if (f.getName().endsWith(".jpg")) {
                    executorService.submit(() -> processImage(f, sessionId));
                }
            } else if (f.isDirectory()) {
                WebSocketServer.sendInfo(JSON.toJSONString(
                                new SocketMessage("200",
                                        f.getName() + " is not a JPEG file, skipping...",
                                        null, null, null)),
                        sessionId);
            }
        }
        return null;
    }

    /**
     * Processes a single image file, extracts features and sends data to the front-end.
     * @param file The file to process.
     * @param sessionId The session ID of the WebSocket connection.
     */
    @SneakyThrows
    private void processImage(File file, String sessionId) {
        FileInputStream fileInputStream = null; // Used to detect faces
        FileInputStream fileInputStreamNewPhoto = null; // Used to process new photos
        FileInputStream fileInputStreamOldPhoto = null; // Used to process old photos        try {
        fileInputStream = new FileInputStream(file);
        List<FaceInfo> faceInfos = faceUtils.detectFaces(fileInputStream);
        if (faceInfos.size() == 0) {
            fileInputStreamOldPhoto = new FileInputStream(file);
            WebSocketServer.sendInfo(JSON.toJSONString(
                            new SocketMessage("300",
                                    "No face detected", Base64Utils.inputStream2Base64(fileInputStreamOldPhoto),
                                    null, null)),
                    sessionId);
            return;
        } else if (faceInfos.size() > 1) {
            fileInputStreamOldPhoto = new FileInputStream(file);
            WebSocketServer.sendInfo(JSON.toJSONString(
                            new SocketMessage("300",
                                    "Multiple faces detected", Base64Utils.inputStream2Base64(fileInputStreamOldPhoto),
                                    null, null)),
                    sessionId);
            return;
        } else {
            fileInputStreamOldPhoto = new FileInputStream(file);
            fileInputStreamNewPhoto = new FileInputStream(file);
            String newImageBase64 = faceUtils.extractFaceImage(fileInputStreamNewPhoto, faceInfos.get(0).getRect());
            String oldImageBase64 = Base64Utils.inputStream2Base64(fileInputStreamOldPhoto);
            if (newImageBase64 == null || newImageBase64.isEmpty()) {
                WebSocketServer.sendInfo(JSON.toJSONString(
                                new SocketMessage("300",
                                        "No face detected", oldImageBase64,
                                        null, null)),
                        sessionId);
                return;
            }
            WebSocketServer.sendInfo(JSON.toJSONString(
                            new SocketMessage("300",
                                    "Face detected", oldImageBase64,
                                    newImageBase64, null)),
                    sessionId);
            byte[] feature = faceUtils.extractFaceFeature(new FileInputStream(file), faceInfos.get(0));
            SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(0, 0);
            String featureKey = snowflakeIdWorker.nextId() + "";
            if (feature.length == 0) {
                return;
            } else {
                redisService.setByKey(Base64Utils.byteArray2Base(feature), RedisService.TIME_ONE_MINUTE * 20, featureKey);
                log.info("Feature extracted successfully, cached ID: " + featureKey + ", expiration time: " + RedisService.TIME_ONE_MINUTE * 20);
            }
            String fileName = file.getName().replace(".jpg", "");
            MessageImageForm images = new MessageImageForm();
            images.setName(fileName);
            images.setImageBase64(newImageBase64);
            images.setFeatureVector(featureKey);
            images.setGender("male");
            jmsMessagingTemplate.convertAndSend(queue, JSON.toJSONString(images));
            log.info("Data sent to queue successfully.");
        }

}
}