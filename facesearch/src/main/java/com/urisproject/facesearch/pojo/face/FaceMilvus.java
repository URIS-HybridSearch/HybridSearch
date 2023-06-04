package com.urisproject.facesearch.pojo.face;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "FaceMilvus Definition")
public class FaceMilvus {
    @Schema(description = "Collection name for face data", example = "face_home", required = true)
    public static final String COLLECTION_NAME = "face_home";

    @Schema(description = "Length of the feature vector for the face data", example = "256", required = true)
    public static final Integer FEATURE_DIM = 256;

    @Schema(description = "Number of results to return in a search", example = "5", required = true)
    public static final Integer SEARCH_K = 5;

    @Schema(description = "Parameters for a search", example = "{\"nprobe\":10}", required = true)
    public static final String SEARCH_PARAM = "{\"nprobe\":10}";

    @Data
    @Schema(name = "Fields for face data")
    public static class Field {
        @Schema(description = "Name of the user", example = "John Doe", required = true)
        public static final String USER_NAME = "user_name";

        @Schema(description = "Code for the user", example = "1234", required = true)
        public static final String USER_CODE = "user_code";

        @Schema(description = "Feature vector for the user's face", example = "[0.1, 0.2, ..., 0.9]", required = true)
        public static final String FEATURE = "user_feature";
    }
}