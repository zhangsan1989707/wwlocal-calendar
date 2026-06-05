package com.wwlocal.calendar.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

@Service
public class CrudService {
  private final JdbcTemplate jdbc;

  public CrudService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(String table, Set<String> allowedFilters, Map<String, String> params, String orderBy) {
    var sql = new StringBuilder("SELECT * FROM ").append(table);
    var args = new ArrayList<>();
    var clauses = new ArrayList<String>();
    params.forEach((key, value) -> {
      if (value != null && !value.isBlank() && allowedFilters.contains(key)) {
        clauses.add(key + " = ?");
        args.add(value);
      }
    });
    if (!clauses.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", clauses));
    }
    sql.append(" ORDER BY ").append(orderBy);
    return jdbc.queryForList(sql.toString(), args.toArray());
  }

  public Map<String, Object> create(String table, Set<String> columns, Map<String, Object> payload) {
    var values = filtered(columns, payload);
    if (values.isEmpty()) {
      throw new IllegalArgumentException("提交内容不能为空");
    }
    var names = new StringJoiner(", ");
    var marks = new StringJoiner(", ");
    var args = new ArrayList<>();
    values.forEach((key, value) -> {
      names.add(key);
      marks.add("?");
      args.add(value);
    });
    var sql = "INSERT INTO " + table + "(" + names + ") VALUES (" + marks + ") RETURNING *";
    return jdbc.queryForMap(sql, args.toArray());
  }

  public Map<String, Object> update(String table, Set<String> columns, Object id, Map<String, Object> payload) {
    var values = filtered(columns, payload);
    values.put("updated_at", Timestamp.from(Instant.now()));
    var sets = new StringJoiner(", ");
    var args = new ArrayList<>();
    values.forEach((key, value) -> {
      sets.add(key + " = ?");
      args.add(value);
    });
    args.add(id);
    return jdbc.queryForMap("UPDATE " + table + " SET " + sets + " WHERE id = ? RETURNING *", args.toArray());
  }

  public void disable(String table, long id) {
    // Uses 'CANCELLED' to comply with event table CHECK constraint (ACTIVE|CANCELLED)
    jdbc.update("UPDATE " + table + " SET status = 'CANCELLED', updated_at = now() WHERE id = ?", id);
  }

  public void delete(String table, long id) {
    jdbc.update("DELETE FROM " + table + " WHERE id = ?", id);
  }

  private Map<String, Object> filtered(Set<String> columns, Map<String, Object> payload) {
    var values = new LinkedHashMap<String, Object>();
    payload.forEach((key, value) -> {
      if (columns.contains(key)) {
        values.put(key, value);
      }
    });
    return values;
  }
}
