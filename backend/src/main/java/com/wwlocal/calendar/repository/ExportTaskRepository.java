package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.ExportTaskEntity;
import com.wwlocal.calendar.dto.Requests.ExportTaskRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ExportTaskRepository extends JdbcRepositorySupport<ExportTaskEntity, ExportTaskRequest> {
    public ExportTaskRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "export_tasks", (rs, rowNum) -> new ExportTaskEntity(
                rs.getString("id"), rs.getString("task_type"), rs.getString("status"), rs.getString("file_url"),
                rs.getString("requested_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<ExportTaskEntity> findAll() {
        return jdbcTemplate.query("select * from export_tasks order by created_at desc", (rs, rowNum) -> new ExportTaskEntity(
                rs.getString("id"), rs.getString("task_type"), rs.getString("status"), rs.getString("file_url"),
                rs.getString("requested_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public ExportTaskEntity create(ExportTaskRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into export_tasks(id, task_type, status, file_url, requested_by)
                values (?, ?, ?, ?, ?)
                """, id, request.taskType(), request.status(), request.fileUrl(), request.requestedBy());
        return findById(id);
    }

    @Override
    public ExportTaskEntity update(String id, ExportTaskRequest request) {
        jdbcTemplate.update("""
                update export_tasks set task_type = ?, status = ?, file_url = ?, requested_by = ?, updated_at = now()
                where id = ?
                """, request.taskType(), request.status(), request.fileUrl(), request.requestedBy(), id);
        return findById(id);
    }
}
