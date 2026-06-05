package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.ExportTaskEntity;
import com.wwlocal.calendar.dto.Requests.ExportTaskRequest;
import com.wwlocal.calendar.service.ExportTaskService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export-tasks")
public class ExportTaskController extends CrudController<ExportTaskEntity, ExportTaskRequest> {
    public ExportTaskController(ExportTaskService service) {
        super(service);
    }
}
