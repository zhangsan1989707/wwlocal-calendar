package com.wwlocal.calendar.repository;

import java.util.List;

public interface CrudRepository<E, R> {
    List<E> findAll();

    E findById(String id);

    E create(R request);

    E update(String id, R request);

    void delete(String id);
}
