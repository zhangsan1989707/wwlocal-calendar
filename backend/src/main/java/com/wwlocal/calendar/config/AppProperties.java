package com.wwlocal.calendar.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String uploadDir,
    String exportDir,
    String backupDir,
    List<String> allowedOrigins
) {
}
