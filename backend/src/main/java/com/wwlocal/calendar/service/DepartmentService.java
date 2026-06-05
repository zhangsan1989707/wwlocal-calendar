package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.DepartmentEntity;
import com.wwlocal.calendar.dto.Requests.DepartmentRequest;
import com.wwlocal.calendar.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService extends BaseCrudService<DepartmentEntity, DepartmentRequest> {
    public DepartmentService(DepartmentRepository repository) {
        super(repository);
    }
}
