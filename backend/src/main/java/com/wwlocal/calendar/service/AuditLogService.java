package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.AuditLogEntity;
import com.wwlocal.calendar.dto.Requests.AuditLogRequest;
import com.wwlocal.calendar.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService extends BaseCrudService<AuditLogEntity, AuditLogRequest> {
    public AuditLogService(AuditLogRepository repository) {
        super(repository);
    }
}
