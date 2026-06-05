package com.wwlocal.calendar.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final JdbcTemplate jdbc;

  public AuditService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void record(Long operatorUserId, String module, String action, String objectType, Object objectId, String summary) {
    jdbc.update("""
        INSERT INTO audit_log(operator_user_id, module, action, object_type, object_id, detail)
        VALUES (?, ?, ?, ?, ?, ?)
        """, operatorUserId, module, action, objectType, objectId, summary);
  }
}
