package com.toanlv.flashsale.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheManager cacheManager(
      RedisConnectionFactory cf, GenericJackson2JsonRedisSerializer jsonSerializer) {

    var defaultConfig = buildDefaultConfig(jsonSerializer);

    var perCacheConfigs =
        Map.of(
            "current-flash-sale", defaultConfig.entryTtl(Duration.ofSeconds(2)),
            "product-catalog", defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "category-tree", defaultConfig.entryTtl(Duration.ofHours(1)));

    return RedisCacheManager.builder(cf)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(perCacheConfigs)
        .transactionAware()
        .build();
  }

  private RedisCacheConfiguration buildDefaultConfig(
      GenericJackson2JsonRedisSerializer jsonSerializer) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(10))
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
        .prefixCacheNameWith("flashsale:cache:");
  }
}
