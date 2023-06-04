package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Upload Result Form")
public class UploadResultForm {
    @Schema(description = "Name of the uploaded image", required = true)
    private String imageName;

    @Schema(description = "Upload result", required = true)
    private String result;

    @Schema(description = "Reason for the upload result")
    private String reason;
}