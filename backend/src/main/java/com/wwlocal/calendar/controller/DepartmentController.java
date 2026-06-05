package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.DepartmentEntity;
import com.wwlocal.calendar.dto.Requests.DepartmentRequest;
import com.wwlocal.calendar.service.DepartmentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/departments")
public class DepartmentController extends CrudController<DepartmentEntity, DepartmentRequest> {
    public DepartmentController(DepartmentService service) {
        super(service);
    }
}
