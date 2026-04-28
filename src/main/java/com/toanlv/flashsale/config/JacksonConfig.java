package com.toanlv.flashsale.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class JacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder ->
        builder.featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            SerializationFeature.FAIL_ON_EMPTY_BEANS,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}
