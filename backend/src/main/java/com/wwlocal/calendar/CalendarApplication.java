package com.wwlocal.calendar;

import com.wwlocal.calendar.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CalendarApplication {
  public static void main(String[] args) {
    SpringApplication.run(CalendarApplication.class, args);
  }
}
