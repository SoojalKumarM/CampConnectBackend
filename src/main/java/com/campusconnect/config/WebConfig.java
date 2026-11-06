package com.campusconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(props.getUploadDir()).toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) location += "/";   // directory must end with a slash
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
