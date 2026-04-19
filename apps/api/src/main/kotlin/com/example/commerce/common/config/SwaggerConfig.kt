package com.example.commerce.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Commerce API")
                .description("쇼핑몰 백엔드 API 문서")
                .version("v1.0.0")
        )
        .addSecurityItem(SecurityRequirement().addList("BearerAuth"))
        .components(
            Components().addSecuritySchemes(
                "BearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("로그인 후 발급된 JWT 토큰을 입력하세요. (Bearer 접두사 불필요)")
            )
        )
}
