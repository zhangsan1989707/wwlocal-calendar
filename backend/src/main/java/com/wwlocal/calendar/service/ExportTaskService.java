package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.ExportTaskEntity;
import com.wwlocal.calendar.dto.Requests.ExportTaskRequest;
import com.wwlocal.calendar.repository.ExportTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class ExportTaskService extends BaseCrudService<ExportTaskEntity, ExportTaskRequest> {
    public ExportTaskService(ExportTaskRepository repository) {
        super(repository);
    }
}
