package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description:
 */
@Data
@Schema(name = "Add User Form")
public class AddUserForm {
    @Schema(description = "Name of the user", required = true)
    private String name;

    @Schema(description = "Gender of the user", required = true)
    private String gender;

    @Schema(description = "Base64-encoded image of the user", required = true)
    private String imageBase64;

    @Schema(description = "Feature vector of the user", required = true)
    private String featureVector;
}
