package com.wwlocal.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class CalendarApplication {
  public static void main(String[] args) {
    SpringApplication.run(CalendarApplication.class, args);
  }
}
