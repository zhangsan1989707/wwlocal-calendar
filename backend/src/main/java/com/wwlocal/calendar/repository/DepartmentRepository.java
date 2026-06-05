package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.DepartmentEntity;
import com.wwlocal.calendar.dto.Requests.DepartmentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DepartmentRepository extends JdbcRepositorySupport<DepartmentEntity, DepartmentRequest> {
    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "departments", (rs, rowNum) -> new DepartmentEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("parent_id"), rs.getInt("sort_order"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<DepartmentEntity> findAll() {
        return jdbcTemplate.query("select * from departments order by sort_order asc, created_at asc", (rs, rowNum) -> new DepartmentEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("parent_id"), rs.getInt("sort_order"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public DepartmentEntity create(DepartmentRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into departments(id, name, parent_id, sort_order)
                values (?, ?, ?, ?)
                """, id, request.name(), request.parentId(), request.sortOrder());
        return findById(id);
    }

    @Override
    public DepartmentEntity update(String id, DepartmentRequest request) {
        jdbcTemplate.update("""
                update departments set name = ?, parent_id = ?, sort_order = ?, updated_at = now()
                where id = ?
                """, request.name(), request.parentId(), request.sortOrder(), id);
        return findById(id);
    }
}
