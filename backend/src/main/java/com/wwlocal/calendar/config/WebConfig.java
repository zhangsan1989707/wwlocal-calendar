package com.wwlocal.calendar.config;

import com.wwlocal.calendar.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AppProperties appProperties;

    public WebConfig(AuthInterceptor authInterceptor, AppProperties appProperties) {
        this.authInterceptor = authInterceptor;
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var origins = appProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/health")
                .order(1);
    }
}
