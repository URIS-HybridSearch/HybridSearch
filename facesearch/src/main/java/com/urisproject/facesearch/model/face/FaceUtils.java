package com.urisproject.facesearch.model.face;




import com.arcsoft.face.*;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.urisproject.facesearch.factory.FaceEnginePoolFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description: Utility class for face recognition.
 */

@Component
@Log4j2
public class FaceUtils {

    private final GenericObjectPool<FaceEngine> faceEnginePool;

    public FaceUtils() {
        // Object pool factory
        FaceEnginePoolFactory faceEnginePoolFactory = new FaceEnginePoolFactory();
        // Object pool configuration
        GenericObjectPoolConfig<FaceEngine> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(5);
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true); // Check for leaks during maintenance
        abandonedConfig.setRemoveAbandonedOnBorrow(true); // Check for leaks when borrowing
        abandonedConfig.setRemoveAbandonedTimeout(10); // Consider an object leaked if not returned within 10 seconds
        // Object pool
        faceEnginePool = new GenericObjectPool<>(faceEnginePoolFactory, poolConfig);
        faceEnginePool.setAbandonedConfig(abandonedConfig);
        faceEnginePool.setTimeBetweenEvictionRunsMillis(5000); // Run maintenance task every 5 seconds
        log.info("Face engine pool opened successfully");
    }

    /**
     * Detect faces in the given image.
     *
     * @param inputStream The input stream of the image
     * @return A list of detected faces
     * @throws IOException If an I/O error occurs
     */
    public List<FaceInfo> detectFaces(InputStream inputStream) throws IOException {
        FaceEngine faceEngine = null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            faceEngine = faceEnginePool.borrowObject();
            IOUtils.copy(inputStream, outputStream);
            ImageInfo imageInfo = ImageFactory.getRGBData(outputStream.toByteArray());
            List<FaceInfo> faceInfoList = new ArrayList<>();
            int errorCode = faceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfoList);
            return faceInfoList;
        } catch (Exception e) {
            log.error("An error occurred while detecting faces", e);
            return new ArrayList<>();
        } finally {
            // Return the engine to the pool
            if (faceEngine != null) {
                faceEnginePool.returnObject(faceEngine);
            }
        }
    }

    /**
     * Extract the face image from the given input stream based on the specified rectangle.
     *
     * @param inputStream The input stream of the image
     * @param rect        The rectangle that specifies the face location
     * @return The base64 encoded string of the face image
     * @throws IOException If an I/O error occurs
     */
    public String extractFaceImage(InputStream inputStream, Rect rect) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            BufferedImage subImage = image.getSubimage(rect.getLeft(), rect.getTop(), rect.getRight() - rect.getLeft(), rect.getBottom() - rect.getTop());
            ImageIO.write(subImage, "png", outputStream);
            byte[] imageData = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageData);
        } catch (Exception e) {
            log.error("An error occurred while extracting face image", e);
            return null;
        }
    }

    /**
     * Extract the feature vector of the specified face.
     *
     * @param inputStream The input stream of the image
     * @param faceInfo    The face to extract the feature vector from
     * @return The feature vector
     * @throws IOException If an I/O error occurs
     */
    public byte[] extractFaceFeature(InputStream inputStream, FaceInfo faceInfo) throws IOException {
        FaceEngine faceEngine = null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            faceEngine = faceEnginePool.borrowObject();
            IOUtils.copy(inputStream, outputStream);
            ImageInfo imageInfo = ImageFactory.getRGBData(outputStream.toByteArray());
            FaceFeature faceFeature = new FaceFeature();
            int errorCode = faceEngine.extractFaceFeature(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getImageFormat(), faceInfo, faceFeature);
            return faceFeature.getFeatureData();
        } catch (Exception e) {
            log.error("An error occurred while extracting face feature",e);
            return new byte[0];
        } finally {
            // Return the engine to the pool
            if (faceEngine != null) {
                faceEnginePool.returnObject(faceEngine);
            }
        }
    }

    /**
     * Compare the feature vectors of two faces.
     *
     * @param source The feature vector of the source face
     * @param target The feature vector of the target face
     * @return The similarity score between the two faces
     * @throws IOException If an I/O error occurs
     */
    public float compareFaceFeatures(byte[] source, byte[] target) throws IOException {
        FaceEngine faceEngine = null;
        try {
            faceEngine = faceEnginePool.borrowObject();
            FaceFeature sourceFaceFeature = new FaceFeature();
            sourceFaceFeature.setFeatureData(source);
            FaceFeature targetFaceFeature = new FaceFeature();
            targetFaceFeature.setFeatureData(target);
            FaceSimilar faceSimilar = new FaceSimilar();
            int errorCode = faceEngine.compareFaceFeature(sourceFaceFeature, targetFaceFeature, faceSimilar);
            return faceSimilar.getScore();
        } catch (Exception e) {
            log.error("An error occurred while comparing face features", e);
            return 0;
        } finally {
            // Return the engine to the pool
            if (faceEngine != null) {
                faceEnginePool.returnObject(faceEngine);
            }
        }
    }
}
