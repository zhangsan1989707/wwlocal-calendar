package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.common.ApiResponse;
import com.wwlocal.calendar.service.BaseCrudService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public abstract class CrudController<E, R> {
    private final BaseCrudService<E, R> service;

    protected CrudController(BaseCrudService<E, R> service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<E>> findAll() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<E> findById(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<E> create(@Valid @RequestBody R request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<E> update(@PathVariable String id, @Valid @RequestBody R request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
