package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.BackupRestoreEntity;
import com.wwlocal.calendar.dto.Requests.BackupRestoreRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BackupRestoreRepository extends JdbcRepositorySupport<BackupRestoreEntity, BackupRestoreRequest> {
    public BackupRestoreRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "backup_restores", (rs, rowNum) -> new BackupRestoreEntity(
                rs.getString("id"), rs.getString("operation_type"), rs.getString("status"), rs.getString("file_url"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<BackupRestoreEntity> findAll() {
        return jdbcTemplate.query("select * from backup_restores order by created_at desc", (rs, rowNum) -> new BackupRestoreEntity(
                rs.getString("id"), rs.getString("operation_type"), rs.getString("status"), rs.getString("file_url"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public BackupRestoreEntity create(BackupRestoreRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into backup_restores(id, operation_type, status, file_url, created_by)
                values (?, ?, ?, ?, ?)
                """, id, request.operationType(), request.status(), request.fileUrl(), request.createdBy());
        return findById(id);
    }

    @Override
    public BackupRestoreEntity update(String id, BackupRestoreRequest request) {
        jdbcTemplate.update("""
                update backup_restores set operation_type = ?, status = ?, file_url = ?, created_by = ?, updated_at = now()
                where id = ?
                """, request.operationType(), request.status(), request.fileUrl(), request.createdBy(), id);
        return findById(id);
    }
}
