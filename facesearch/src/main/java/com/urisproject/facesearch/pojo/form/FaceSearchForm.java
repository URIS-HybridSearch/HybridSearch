package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Face Search Form")
public class FaceSearchForm {
    @Schema(description = "Session ID", required = true)
    private String sessionId;

    @Schema(description = "Base64-encoded image to search for", required = true)
    private String imageBase64;

    @Schema(description = "Message to include with search results")
    private String message;
}
