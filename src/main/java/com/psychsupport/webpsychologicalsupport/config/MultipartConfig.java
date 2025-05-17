package com.psychsupport.webpsychologicalsupport.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.http.MediaType;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig implements WebMvcConfigurer {

    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(false);
        return resolver;
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(10));
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2));
        factory.setLocation("uploads");
        return factory.createMultipartConfig();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("multipart", MediaType.MULTIPART_FORM_DATA);
        configurer.mediaType("form", MediaType.MULTIPART_FORM_DATA);
        configurer.mediaType("multipart/form-data", MediaType.MULTIPART_FORM_DATA);
    }
} 