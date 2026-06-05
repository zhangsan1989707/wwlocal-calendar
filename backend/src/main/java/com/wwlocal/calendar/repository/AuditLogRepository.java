package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.AuditLogEntity;
import com.wwlocal.calendar.dto.Requests.AuditLogRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogRepository extends JdbcRepositorySupport<AuditLogEntity, AuditLogRequest> {
    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "audit_logs", (rs, rowNum) -> new AuditLogEntity(
                rs.getString("id"), rs.getString("actor_user_id"), rs.getString("action"),
                rs.getString("target_type"), rs.getString("target_id"), rs.getString("detail"),
                rs.getTimestamp("created_at").toInstant()
        ));
    }

    @Override
    public List<AuditLogEntity> findAll() {
        return jdbcTemplate.query("select * from audit_logs order by created_at desc", (rs, rowNum) -> new AuditLogEntity(
                rs.getString("id"), rs.getString("actor_user_id"), rs.getString("action"),
                rs.getString("target_type"), rs.getString("target_id"), rs.getString("detail"),
                rs.getTimestamp("created_at").toInstant()
        ));
    }

    @Override
    public AuditLogEntity create(AuditLogRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into audit_logs(id, actor_user_id, action, target_type, target_id, detail)
                values (?, ?, ?, ?, ?, ?)
                """, id, request.actorUserId(), request.action(), request.targetType(), request.targetId(), request.detail());
        return findById(id);
    }

    @Override
    public AuditLogEntity update(String id, AuditLogRequest request) {
        jdbcTemplate.update("""
                update audit_logs set actor_user_id = ?, action = ?, target_type = ?, target_id = ?, detail = ?
                where id = ?
                """, request.actorUserId(), request.action(), request.targetType(), request.targetId(), request.detail(), id);
        return findById(id);
    }
}
