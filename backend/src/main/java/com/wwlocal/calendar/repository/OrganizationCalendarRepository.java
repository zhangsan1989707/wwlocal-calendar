package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.domain.Entities.OrganizationCalendarEntity;
import com.wwlocal.calendar.dto.Requests.OrganizationCalendarRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OrganizationCalendarRepository extends JdbcRepositorySupport<OrganizationCalendarEntity, OrganizationCalendarRequest> {
    public OrganizationCalendarRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "organization_calendars", (rs, rowNum) -> new OrganizationCalendarEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("scope_type"), rs.getString("color"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public List<OrganizationCalendarEntity> findAll() {
        return jdbcTemplate.query("select * from organization_calendars order by created_at desc", (rs, rowNum) -> new OrganizationCalendarEntity(
                rs.getString("id"), rs.getString("name"), rs.getString("scope_type"), rs.getString("color"),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    @Override
    public OrganizationCalendarEntity create(OrganizationCalendarRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into organization_calendars(id, name, scope_type, color, created_by)
                values (?, ?, ?, ?, ?)
                """, id, request.name(), request.scopeType(), request.color(), request.createdBy());
        return findById(id);
    }

    @Override
    public OrganizationCalendarEntity update(String id, OrganizationCalendarRequest request) {
        jdbcTemplate.update("""
                update organization_calendars set name = ?, scope_type = ?, color = ?, created_by = ?, updated_at = now()
                where id = ?
                """, request.name(), request.scopeType(), request.color(), request.createdBy(), id);
        return findById(id);
    }
}
