package com.wwlocal.calendar.service;

import com.wwlocal.calendar.domain.Entities.SystemConfigEntity;
import com.wwlocal.calendar.dto.Requests.SystemConfigRequest;
import com.wwlocal.calendar.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService extends BaseCrudService<SystemConfigEntity, SystemConfigRequest> {
    public SystemConfigService(SystemConfigRepository repository) {
        super(repository);
    }
}
