package com.urisproject.facesearch.pojo.form;

import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.Rect;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Message Image Form")
public class MessageImageForm {
    @Schema(description = "Name of the file", required = true)
    private String name;

    @Schema(description = "Gender of the person in the image")
    private String gender;

    @Schema(description = "Base64-encoded image data", required = true)
    private String imageBase64;

    @Schema(description = "Feature vector of the face in the image")
    private String featureVector;
}