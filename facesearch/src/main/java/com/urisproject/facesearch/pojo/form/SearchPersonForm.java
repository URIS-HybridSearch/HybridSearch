package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Search Person Form")
public class SearchPersonForm {
    @Schema(description = "Feature vector for the face to search for", required = true)
    private String featureVector;
}