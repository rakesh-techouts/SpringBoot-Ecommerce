package com.techouts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthSessionInterceptor authSessionInterceptor;

    public WebMvcConfig(AuthSessionInterceptor authSessionInterceptor) {
        this.authSessionInterceptor = authSessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authSessionInterceptor)
                .addPathPatterns("/home", "/cart/**", "/checkout/**", "/orders", "/buy-now", "/profile", "/order-success");
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}
