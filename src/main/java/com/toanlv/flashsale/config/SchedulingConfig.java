package com.toanlv.flashsale.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    var scheduler = new SimpleAsyncTaskScheduler();
    scheduler.setVirtualThreads(true);
    scheduler.setThreadNamePrefix("scheduler-");
    registrar.setTaskScheduler(scheduler);
  }
}
