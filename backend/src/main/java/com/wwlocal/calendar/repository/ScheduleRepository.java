package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.ScheduleEntity;
import com.wwlocal.calendar.dto.Requests.ScheduleRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class ScheduleRepository extends JdbcRepositorySupport<ScheduleEntity, ScheduleRequest> {
    public ScheduleRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "schedules", (rs, rowNum) -> new ScheduleEntity(
                rs.getString("id"), rs.getString("title"), rs.getString("calendar_id"),
                rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant(),
                rs.getString("location"), rs.getString("description"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<ScheduleEntity> findAll() {
        return jdbcTemplate.query("select * from schedules order by start_at asc", (rs, rowNum) -> new ScheduleEntity(
                rs.getString("id"), rs.getString("title"), rs.getString("calendar_id"),
                rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant(),
                rs.getString("location"), rs.getString("description"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public ScheduleEntity create(ScheduleRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into schedules(id, title, calendar_id, start_at, end_at, location, description, created_by)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, request.title(), request.calendarId(), Timestamp.from(request.startAt()),
                Timestamp.from(request.endAt()), request.location(), request.description(), request.createdBy());
        return findById(id);
    }

    @Override
    public ScheduleEntity update(String id, ScheduleRequest request) {
        jdbcTemplate.update("""
                update schedules set title = ?, calendar_id = ?, start_at = ?, end_at = ?, location = ?,
                    description = ?, created_by = ?, updated_at = now()
                where id = ?
                """, request.title(), request.calendarId(), Timestamp.from(request.startAt()),
                Timestamp.from(request.endAt()), request.location(), request.description(), request.createdBy(), id);
        return findById(id);
    }
}
