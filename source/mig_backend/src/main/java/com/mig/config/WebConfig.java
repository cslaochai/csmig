package com.mig.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册登录鉴权拦截器。
 *
 * <p>拦截范围：管理后台 /admin/** 与 /auth/info、/auth/logout 等需登录接口；
 * 放行：登录公钥 /auth/publicKey、登录 /auth/login、HIS 网关转发 /gateway/**、/insure/**（机器对机器）。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/publicKey",
                        "/auth/login",
                        "/gateway/**",
                        "/insure/**",
                        "/error",
                        "/favicon.ico"
                );
    }
}
