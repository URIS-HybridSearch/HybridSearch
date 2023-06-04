package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "File Upload Form")
public class FileUploadForm {
    @Schema(description = "Session ID for the upload", required = true)
    private String sessionId;
}