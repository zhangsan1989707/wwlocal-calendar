package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.SystemConfigEntity;
import com.wwlocal.calendar.dto.Requests.SystemConfigRequest;
import com.wwlocal.calendar.service.SystemConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-configs")
public class SystemConfigController extends CrudController<SystemConfigEntity, SystemConfigRequest> {
    public SystemConfigController(SystemConfigService service) {
        super(service);
    }
}
