package com.urisproject.facesearch.model.face;


import com.arcsoft.face.FaceInfo;
import com.urisproject.facesearch.common.redis.RedisService;
import com.urisproject.facesearch.common.util.Base64Utils;
import com.urisproject.facesearch.common.util.SnowflakeIdWorker;
import com.urisproject.facesearch.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Log4j2
public class FaceSearchHandler { // Class name should start with a capital letter
    @Autowired
    private FaceUtils faceUtils; // Variable name should start with a lowercase letter
    @Autowired
    private RedisService redisService;
    @Autowired
    private FaceSearchUtils faceSearch;

    public List<User> faceHandler(String imageBase64) throws IOException { // Method name should start with a lowercase letter and use camelCase
        // Face detection first
        InputStream inputStream = Base64Utils.base2InputStream(imageBase64);
        InputStream featureStream = Base64Utils.base2InputStream(imageBase64);
        List<FaceInfo> faceInfos;
        try {
            faceInfos = faceUtils.detectFaces(inputStream);
            if (faceInfos.isEmpty()) {
                log.info("No faces detected in the image.");
            } else {
                // Extracting face features
                byte[] feature = faceUtils.extractFaceFeature(featureStream, faceInfos.get(0));
                if (feature == null || feature.length == 0) {
                    log.error("Failed to extract face feature.");
                } else {
                    SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(0, 0);
                    String featureKey = String.valueOf(snowflakeIdWorker.nextId()); // Use String.valueOf instead of "" + to convert long to string
                    redisService.setByKey(Base64Utils.byteArray2Base(feature), RedisService.TIME_ONE_SECOND * 10, featureKey);
                    log.info("User found in the front-end search.");
                    return faceSearch.getPersonListByPhoto(featureKey);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            inputStream.close();
            featureStream.close();
        }
        return new ArrayList<>(); // This line is unnecessary since the previous return statement will always be executed
    }
}