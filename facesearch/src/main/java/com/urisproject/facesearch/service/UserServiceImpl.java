package com.urisproject.facesearch.service;



import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import com.urisproject.facesearch.common.utils.AliyunOSSUtil;
import com.urisproject.facesearch.model.User;
import com.urisproject.facesearch.model.face.FaceUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Service implementation class for managing users.
 */
@Service
@Log4j2
public class UserServiceImpl implements UserService {

    @Value("${milvus.collection}")
    private String collection;

    @Value("${milvus.partition}")
    private String partition;


    @Autowired
    AliyunOSSUtil aliyunOSSUtil;

    @Autowired
    FaceUtils faceUtils;

    @Autowired
    RedisService redisService;

    @Autowired
    MilvusOperateUtils milvus;

    @Override
    public String addUser(String name, String gender, String imageBase64, String feature) {
        long userId = 0;
        try {
            userId = generateUserId();
        } catch (Exception e) {
            log.error("Unable to generate user ID", e);
            return null;
        }

        String featureKey = (String) redisService.getByKey(feature);
        if (featureKey == null || featureKey.isEmpty()) {
            log.info("Redis cache for feature key not found");
            return null;
        }

        byte[] featureBytes = Base64Utils.Base2byteArray(featureKey);
        List<List<Float>> featureList = new ArrayList<>();
        List<Float> featureFloats = ByteUtils.byteArray2List(featureBytes);
        featureList.add(featureFloats);
        log.info("Feature vector length: " + featureFloats.size());

        long id = milvus.insert(collection, partition, userId, userId, featureList);
        if (id == 0) {
            log.error("Failed to store feature vector in Milvus");
            return null;
        }
        log.info("Feature vector stored in Milvus with ID: " + id);

        boolean redisSetSuccess = redisService.setByKey(featureKey, String.valueOf(userId));
        if (redisSetSuccess) {
            log.info("Cached face data in Redis");
        } else {
            log.info("Redis cache failed, program will continue running");
        }

        byte[] imageBytes = Base64.decode(imageBase64);
        String filePath = aliyunOSSUtil.upload(imageBytes, String.valueOf(userId));
        if (filePath != null) {
            log.info("File uploaded successfully. File path: " + filePath);
            User user = new User();
            user.setUserCode(String.valueOf(userId));
            user.setUserSex(gender);
            user.setUserName(name);
            user.setUserImage(filePath);
            int insertCount = userMapper.insert(user);
            if (insertCount > 0) {
                return filePath;
            } else {
                log.error("Failed to insert user data into database");
                return null;
            }
        } else {
            log.error("Failed to upload file to Aliyun OSS");
            return null;
        }
    }

    /**
     * Generates a unique user ID using Snowflake algorithm.
     *
     * @return the generated user ID
     * @throws Exception if there is an error generating the ID
     */
    private long generateUserId() throws Exception {
        long dataCenterId = 0;
        long machineId = 0;
        return new SnowflakeIdWorker(dataCenterId, machineId).nextId();
    }
}