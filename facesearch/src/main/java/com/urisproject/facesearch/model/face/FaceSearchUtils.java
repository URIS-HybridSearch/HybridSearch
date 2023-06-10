package com.urisproject.facesearch.model.face;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.urisproject.facesearch.common.redis.RedisService;
import com.urisproject.facesearch.common.util.Base64Utils;
import com.urisproject.facesearch.common.util.ByteUtils;
import com.urisproject.facesearch.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 10/6/2023
 * @Description:
 */
@Component
@Log4j2
public class FaceSearchUtils {
    @Value("${milvus.collection}")
    private String collection;
    @Value("${milvus.partition}")
    private String partition;
    @Value("${oos.endpoint}")
    private String endpoint;
    @Value("${oos.bucketname}")
    private String bucketname;

    @Autowired
    private RedisService redisService;
    @Autowired
    private MilvusOperateUtils milvus;
    @Autowired
    private FaceUtils faceTools;
    @Autowired
    private UserMapper userMapper;

    /**
     * Utility class for searching people by photo on a webpage
     * @param feature
     * @return
     * @throws IOException
     */
    public List<User> getPersonListByPhoto(String feature) throws IOException {
        String cachedFeature = (String) redisService.getByKey(feature); // Feature value stored in Redis
        if (cachedFeature == null || cachedFeature == "") {
            log.error("Some reason caused Redis cache to expire");
            return null;
        }
        byte[] resource = Base64Utils.Base2byteArray(cachedFeature); // Received
        List<List<Float>> lists = new ArrayList<>();
        List<Float> featureFloats = ByteUtils.byteArray2List(resource);
        lists.add(featureFloats);
        log.info("Loading memory");
        milvus.loadingLocation(collection);
        log.info("Loading finished");
        List<?> idList = milvus.searchByFeature(collection, lists);
        List<User> searchResultList = new ArrayList<>();
        for (Object id : idList) {
            String userID = id.toString();
            String featureStr = redisService.getByKey(userID); // Feature vector
            if (featureStr == null || featureStr == "") {
                log.info("Failed to load feature vector from cache, downloading image again");
            } else {
                log.info("Feature vector loaded successfully, calling Arcsoft SDK for face comparison");
                byte[] searchByte = Base64Utils.Base2byteArray(featureStr);
                float source = faceTools.faceCompared(resource, searchByte);
                if (source > 0.8) {
                    log.info("Same person found, getting URL: " + source + "-" + id);
                    QueryWrapper<User> wrapper = new QueryWrapper<>();
                    wrapper.eq("user_code", userID);
                    User user = userMapper.selectOne(wrapper);
                    if (user != null) {
                        String url = "https:" + bucketname + "." + endpoint + "/" + user.getUserImage();
                        user.setUserImage(url);
                        searchResultList.add(user);
                    }
                } else {
                    log.info("Different person found: " + source + "-" + id);
                }
            }
        }
        return searchResultList;
    }
}