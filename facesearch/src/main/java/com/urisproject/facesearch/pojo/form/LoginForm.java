package com.urisproject.facesearch.pojo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Login Form")
public class LoginForm {
    @Schema(description = "Username", required = true)
    private String username;

    @Schema(description = "Password for the user", required = true)
    private String password;
}