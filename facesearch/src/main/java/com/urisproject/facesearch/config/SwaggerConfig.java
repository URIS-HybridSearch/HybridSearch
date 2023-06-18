package com.urisproject.facesearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 18/6/2023
 * @Description:
 */

@Configuration
@EnableSwagger2 // Enable Swagger 2.0 API documentation
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2) // Use Swagger 2.0 specification
                .select()
                .build()
                .apiInfo(apiInfo()); // Set custom API information
    }

    private ApiInfo apiInfo() {
        // Set API title, description, contact information, etc.
        return new ApiInfoBuilder()
                .title("Large-scale Face Search Service")
                .description("API documentation for the Large-scale Face Search Service")
                .version("1.0.0")
                .contact(new Contact("Your Name", "https://www.example.com", "you@example.com"))
                .build();
    }
}
