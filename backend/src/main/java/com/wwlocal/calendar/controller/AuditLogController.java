package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.AuditLogEntity;
import com.wwlocal.calendar.dto.Requests.AuditLogRequest;
import com.wwlocal.calendar.service.AuditLogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/audit-logs")
public class AuditLogController extends CrudController<AuditLogEntity, AuditLogRequest> {
    public AuditLogController(AuditLogService service) {
        super(service);
    }
}
