package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Image Form")
public class ImageForm {
    @Schema(description = "Name of the file", required = true)
    private String fileName;

    @Schema(description = "Base64-encoded image data", required = true)
    private String imageBase64;
}