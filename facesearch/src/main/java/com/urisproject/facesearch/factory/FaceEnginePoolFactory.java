package com.urisproject.facesearch.factory;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.EngineConfiguration;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectOrient;
import com.arcsoft.face.enums.ErrorInfo;

import com.urisproject.facesearch.config.FaceEngineConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description:
 */

@Log4j2
@Component
public class FaceEnginePoolFactory extends BasePooledObjectFactory<FaceEngine> {

    @Override
    public FaceEngine create() throws Exception {
        FaceEngine faceEngine = new FaceEngine(FaceEngineConfig.LIB);

        // Activate the engine
        int errorCode = faceEngine.activeOnline(FaceEngineConfig.APPID, FaceEngineConfig.SDKKEY);
        if (errorCode != ErrorInfo.MOK.getValue() && errorCode != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED.getValue()) {
            log.error("Failed to activate the engine");
        }

        // Get the active file info
        ActiveFileInfo activeFileInfo=new ActiveFileInfo();
        errorCode = faceEngine.getActiveFileInfo(activeFileInfo);
        if (errorCode != ErrorInfo.MOK.getValue() && errorCode != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED.getValue()) {
            log.error("Failed to get the active file info");
        }

        // Configure the engine
        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setDetectMode(DetectMode.ASF_DETECT_MODE_IMAGE);
        engineConfiguration.setDetectFaceOrientPriority(DetectOrient.ASF_OP_ALL_OUT);
        engineConfiguration.setDetectFaceMaxNum(10);
        engineConfiguration.setDetectFaceScaleVal(16);

        // Configure the functions
        FunctionConfiguration functionConfiguration = new FunctionConfiguration();
        functionConfiguration.setSupportAge(true);
        functionConfiguration.setSupportFace3dAngle(true);
        functionConfiguration.setSupportFaceDetect(true);
        functionConfiguration.setSupportFaceRecognition(true);
        functionConfiguration.setSupportGender(true);
        functionConfiguration.setSupportLiveness(true);
        functionConfiguration.setSupportIRLiveness(true);
        engineConfiguration.setFunctionConfiguration(functionConfiguration);

        // Initialize the engine
        errorCode = faceEngine.init(engineConfiguration);
        if (errorCode != ErrorInfo.MOK.getValue()) {
            log.error("Failed to initialize the engine");
        }

        return faceEngine;
    }

    @Override
    public PooledObject<FaceEngine> wrap(FaceEngine faceEngine) {
        return new DefaultPooledObject<>(faceEngine);
    }

    @Override
    public void destroyObject(PooledObject<FaceEngine> faceEngine) throws Exception {
        super.destroyObject(faceEngine);
    }

    @Override
    public boolean validateObject(PooledObject<FaceEngine> faceEngine) {
        return super.validateObject(faceEngine);
    }

    @Override
    public void activateObject(PooledObject<FaceEngine> faceEngine) throws Exception {
        super.activateObject(faceEngine);
    }

    @Override
    public void passivateObject(PooledObject<FaceEngine> faceEngine) throws Exception {
        super.passivateObject(faceEngine);
    }
}