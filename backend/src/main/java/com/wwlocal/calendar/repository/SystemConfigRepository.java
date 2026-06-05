package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.SystemConfigEntity;
import com.wwlocal.calendar.dto.Requests.SystemConfigRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class SystemConfigRepository extends JdbcRepositorySupport<SystemConfigEntity, SystemConfigRequest> {
    public SystemConfigRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "system_configs", (rs, rowNum) -> new SystemConfigEntity(
                rs.getString("id"), rs.getString("config_key"), rs.getString("config_value"),
                rs.getString("description"), rs.getString("updated_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<SystemConfigEntity> findAll() {
        return jdbcTemplate.query("select * from system_configs order by config_key asc", (rs, rowNum) -> new SystemConfigEntity(
                rs.getString("id"), rs.getString("config_key"), rs.getString("config_value"),
                rs.getString("description"), rs.getString("updated_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public SystemConfigEntity create(SystemConfigRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into system_configs(id, config_key, config_value, description, updated_by)
                values (?, ?, ?, ?, ?)
                """, id, request.configKey(), request.configValue(), request.description(), request.updatedBy());
        return findById(id);
    }

    @Override
    public SystemConfigEntity update(String id, SystemConfigRequest request) {
        jdbcTemplate.update("""
                update system_configs set config_key = ?, config_value = ?, description = ?, updated_by = ?, updated_at = now()
                where id = ?
                """, request.configKey(), request.configValue(), request.description(), request.updatedBy(), id);
        return findById(id);
    }
}
