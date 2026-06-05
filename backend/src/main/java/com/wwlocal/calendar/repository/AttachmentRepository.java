package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.AttachmentEntity;
import com.wwlocal.calendar.dto.Requests.AttachmentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AttachmentRepository extends JdbcRepositorySupport<AttachmentEntity, AttachmentRequest> {
    public AttachmentRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "attachments", (rs, rowNum) -> new AttachmentEntity(
                rs.getString("id"), rs.getString("schedule_id"), rs.getString("file_name"),
                rs.getString("file_url"), rs.getLong("file_size"), rs.getString("uploaded_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<AttachmentEntity> findAll() {
        return jdbcTemplate.query("select * from attachments order by created_at desc", (rs, rowNum) -> new AttachmentEntity(
                rs.getString("id"), rs.getString("schedule_id"), rs.getString("file_name"),
                rs.getString("file_url"), rs.getLong("file_size"), rs.getString("uploaded_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public AttachmentEntity create(AttachmentRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into attachments(id, schedule_id, file_name, file_url, file_size, uploaded_by)
                values (?, ?, ?, ?, ?, ?)
                """, id, request.scheduleId(), request.fileName(), request.fileUrl(), request.fileSize(), request.uploadedBy());
        return findById(id);
    }

    @Override
    public AttachmentEntity update(String id, AttachmentRequest request) {
        jdbcTemplate.update("""
                update attachments set schedule_id = ?, file_name = ?, file_url = ?, file_size = ?, uploaded_by = ?,
                    updated_at = now()
                where id = ?
                """, request.scheduleId(), request.fileName(), request.fileUrl(), request.fileSize(), request.uploadedBy(), id);
        return findById(id);
    }
}
