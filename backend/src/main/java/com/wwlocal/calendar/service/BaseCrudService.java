package com.wwlocal.calendar.service;

import com.wwlocal.calendar.repository.CrudRepository;

import java.util.List;

public abstract class BaseCrudService<E, R> {
    private final CrudRepository<E, R> repository;

    protected BaseCrudService(CrudRepository<E, R> repository) {
        this.repository = repository;
    }

    public List<E> findAll() {
        return repository.findAll();
    }

    public E findById(String id) {
        return repository.findById(id);
    }

    public E create(R request) {
        return repository.create(request);
    }

    public E update(String id, R request) {
        return repository.update(id, request);
    }

    public void delete(String id) {
        repository.delete(id);
    }
}
