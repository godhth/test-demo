package com.webox.webox.config;
import com.webox.webox.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired private AuthInterceptor authInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .excludePathPatterns(
                "/", "/login", "/register", "/logout",
                "/css/**", "/js/**", "/img/**",
                "/h2-console/**",
                "/error", "/error/**",
                "/favicon.ico"
            );
    }
}
