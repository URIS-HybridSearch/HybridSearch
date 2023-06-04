package com.urisproject.facesearch.config;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description:
 */
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
public  class FaceEngineConfig {
    public static final String APPID = "BgRPqreUXUx7vxPEHMjoyqv4c4sxj2YyJVav5pHPGmG9";
    public static final String SDKKEY = "GeCadRzrgc226iEnjoB93zutB3LePz69Z6TX2YfsoDUN";
    //public static final String SDKKEY = " ";//linux
    public static final String LIB = "F:\\HybridSearch\\ArcSoft_ArcFace_Java_Windows_x64_V3.0\\libs\\WIN64";
    //public static final String LIB = "/faceCorn/eneige/libs/LINUX64"; // linux
}
