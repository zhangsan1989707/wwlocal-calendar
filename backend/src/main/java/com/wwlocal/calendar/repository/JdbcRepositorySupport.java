package com.wwlocal.calendar.repository;

import com.wwlocal.calendar.common.NotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public abstract class JdbcRepositorySupport<E, R> implements CrudRepository<E, R> {
    protected final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final RowMapper<E> rowMapper;

    protected JdbcRepositorySupport(JdbcTemplate jdbcTemplate, String tableName, RowMapper<E> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.rowMapper = rowMapper;
    }

    @Override
    public E findById(String id) {
        try {
            return jdbcTemplate.queryForObject("select * from " + tableName + " where id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException ex) {
            throw new NotFoundException("资源不存在");
        }
    }

    @Override
    public void delete(String id) {
        int rows = jdbcTemplate.update("delete from " + tableName + " where id = ?", id);
        if (rows == 0) {
            throw new NotFoundException("资源不存在");
        }
    }

    protected Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
