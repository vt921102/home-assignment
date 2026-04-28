package com.toanlv.flashsale.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
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

  /**
   * Register Jackson HTTP message converter using the application's configured ObjectMapper (from
   * JacksonConfig).
   *
   * <p>Ensures consistent serialization behaviour between: - HTTP responses (controllers) - Redis
   * cache (CacheConfig / RedisConfig)
   *
   * <p>Placed first in the converter list so Spring picks it over any auto-configured defaults.
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    var converter = new MappingJackson2HttpMessageConverter(objectMapper);
    converter.setSupportedMediaTypes(
        List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
    converters.add(0, converter);
  }

  /** Default content negotiation — always JSON unless explicitly requested otherwise. */
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
        .defaultContentType(MediaType.APPLICATION_JSON)
        .favorParameter(false)
        .ignoreAcceptHeader(false);
  }
}
