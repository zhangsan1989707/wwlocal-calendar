package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.UserEntity;
import com.wwlocal.calendar.dto.Requests.UserRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class UserRepository extends JdbcRepositorySupport<UserEntity, UserRequest> {
    public UserRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "users", (rs, rowNum) -> new UserEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("email"), rs.getString("mobile"),
                rs.getString("department_id"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<UserEntity> findAll() {
        return jdbcTemplate.query("select * from users order by created_at desc", (rs, rowNum) -> new UserEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("email"), rs.getString("mobile"),
                rs.getString("department_id"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public UserEntity create(UserRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into users(id, name, email, mobile, department_id, status)
                values (?, ?, ?, ?, ?, ?)
                """, id, request.name(), request.email(), request.mobile(), request.departmentId(), request.status());
        return findById(id);
    }

    @Override
    public UserEntity update(String id, UserRequest request) {
        jdbcTemplate.update("""
                update users set name = ?, email = ?, mobile = ?, department_id = ?, status = ?, updated_at = now()
                where id = ?
                """, request.name(), request.email(), request.mobile(), request.departmentId(), request.status(), id);
        return findById(id);
    }
}
