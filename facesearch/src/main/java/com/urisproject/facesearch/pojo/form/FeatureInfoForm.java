package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "Photo Information Form")
public class FeatureInfoForm {
    @Schema(description = "Base64-encoded old photo", required = true)
    private String oldImageBase64;

    @Schema(description = "Base64-encoded new photo", required = true)
    private String newImageBase64;

    @Schema(description = "Key of the session where the feature vector is stored", required = true)
    private String featureSessionKey;
}