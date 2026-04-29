package com.toanlv.flashsale.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

  private final ObjectMapper objectMapper;

  public WebConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // Required by springdoc-openapi for serving the OpenAPI spec file.
    // Without this, springdoc cannot write binary responses (spec download)
    // and Swagger UI may fail to load the spec.
    converters.add(new ByteArrayHttpMessageConverter());

    var converter = new MappingJackson2HttpMessageConverter(objectMapper);
    converter.setSupportedMediaTypes(
        List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
    converters.add(converter);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        .defaultContentType(MediaType.APPLICATION_JSON)
        .favorParameter(false)
        .ignoreAcceptHeader(false);
  }
}
