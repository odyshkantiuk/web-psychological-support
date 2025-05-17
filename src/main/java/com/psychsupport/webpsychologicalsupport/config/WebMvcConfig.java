package com.psychsupport.webpsychologicalsupport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("multipart", MediaType.MULTIPART_FORM_DATA);
        configurer.mediaType("form", MediaType.MULTIPART_FORM_DATA);
        configurer.mediaType("multipart/form-data", MediaType.MULTIPART_FORM_DATA);
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
} 