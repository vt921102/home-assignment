package com.toanlv.flashsale.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.toanlv.flashsale.common.security.AuthenticatedUser;

@Configuration
@EnableJpaRepositories(basePackages = "com.toanlv.flashsale")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {

  @Bean
  public AuditorAware<UUID> auditorProvider() {
    return () -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated()) {
        return Optional.empty();
      }
      if (auth.getPrincipal() instanceof AuthenticatedUser user) {
        return Optional.of(user.userId());
      }
      return Optional.empty();
    };
  }
}
