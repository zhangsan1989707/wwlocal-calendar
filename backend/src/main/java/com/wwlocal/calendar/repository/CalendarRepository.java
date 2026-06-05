package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.CalendarEntity;
import com.wwlocal.calendar.dto.Requests.CalendarRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class CalendarRepository extends JdbcRepositorySupport<CalendarEntity, CalendarRequest> {
    public CalendarRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "calendars", (rs, rowNum) -> new CalendarEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("owner_user_id"), rs.getString("type"),
                rs.getString("color"), rs.getBoolean("visible"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<CalendarEntity> findAll() {
        return jdbcTemplate.query("select * from calendars order by created_at desc", (rs, rowNum) -> new CalendarEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("owner_user_id"), rs.getString("type"),
                rs.getString("color"), rs.getBoolean("visible"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public CalendarEntity create(CalendarRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into calendars(id, name, owner_user_id, type, color, visible)
                values (?, ?, ?, ?, ?, ?)
                """, id, request.name(), request.ownerUserId(), request.type(), request.color(), request.visible());
        return findById(id);
    }

    @Override
    public CalendarEntity update(String id, CalendarRequest request) {
        jdbcTemplate.update("""
                update calendars set name = ?, owner_user_id = ?, type = ?, color = ?, visible = ?, updated_at = now()
                where id = ?
                """, request.name(), request.ownerUserId(), request.type(), request.color(), request.visible(), id);
        return findById(id);
    }
}
