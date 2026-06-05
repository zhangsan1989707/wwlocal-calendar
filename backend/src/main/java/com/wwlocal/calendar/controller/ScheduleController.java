package com.wwlocal.calendar.controller;

import com.wwlocal.calendar.domain.Entities.ScheduleEntity;
import com.wwlocal.calendar.dto.Requests.ScheduleRequest;
import com.wwlocal.calendar.service.ScheduleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController extends CrudController<ScheduleEntity, ScheduleRequest> {
    public ScheduleController(ScheduleService service) {
        super(service);
    }
}
